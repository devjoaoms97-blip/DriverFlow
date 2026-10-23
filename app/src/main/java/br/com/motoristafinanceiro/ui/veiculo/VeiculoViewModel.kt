package br.com.motoristafinanceiro.ui.veiculo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.KmPessoal
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.ConferenciaKm
import br.com.motoristafinanceiro.domain.Odometro
import br.com.motoristafinanceiro.domain.ResumoVeiculo
import br.com.motoristafinanceiro.domain.calcularAlertas
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import br.com.motoristafinanceiro.domain.calcularResumoVeiculo
import br.com.motoristafinanceiro.domain.ultimaLeitura
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class VeiculoUiState(
    val resumo: ResumoVeiculo = ResumoVeiculo(null, null, null, null),
    /** Mais recentes primeiro. */
    val abastecimentos: List<Abastecimento> = emptyList(),
    /** Mais recentes primeiro. */
    val manutencoes: List<Manutencao> = emptyList(),
    /** Um por serviço, mais urgentes primeiro. */
    val alertas: List<AlertaManutencao> = emptyList(),
    /** Depreciação por km usada no custo por km (0 = ignorada). */
    val depreciacaoPorKmCentavos: Long = 0,
    /** Odômetro dos alertas: última leitura + km dos ganhos depois dela. Null sem nenhuma leitura. */
    val odometro: Odometro? = null,
    /** Km pessoais, mais recentes primeiro. */
    val kmPessoais: List<KmPessoal> = emptyList(),
    /** Km do odômetro × km lançados; null sem duas leituras para comparar. */
    val conferencia: ConferenciaKm? = null,
)

/** Regra pura da tela (sem Android), coberta por testes. */
internal fun montarEstadoVeiculo(
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    ganhosMes: List<Ganho>,
    despesasMes: List<Despesa>,
    depreciacaoPorKmCentavos: Long = 0,
    /** Com os km dos ganhos; null usa só as leituras (sem estimativa). */
    odometroEstimado: Odometro? = null,
    /** Todos os km pessoais; o custo por km usa só os do mês de [hoje]. */
    kmPessoais: List<KmPessoal> = emptyList(),
    conferencia: ConferenciaKm? = null,
    hoje: LocalDate = LocalDate.now(),
): VeiculoUiState {
    val mes = YearMonth.from(hoje)
    val resumo = calcularResumoVeiculo(
        abastecimentos,
        manutencoes,
        calcularEstatisticas(
            ganhosMes, despesasMes, depreciacaoPorKmCentavos, kmPessoais.filter { YearMonth.from(it.data) == mes },
        ),
    )
    val odometro = odometroEstimado ?: ultimaLeitura(abastecimentos, manutencoes)?.let { Odometro(it.km, it) }
    return VeiculoUiState(
        resumo = resumo,
        abastecimentos = abastecimentos.sortedWith(
            compareByDescending<Abastecimento> { it.data.toEpochDay() }.thenByDescending { it.odometroKm }
        ),
        manutencoes = manutencoes.sortedWith(
            compareByDescending<Manutencao> { it.data.toEpochDay() }.thenByDescending { it.odometroKm }
        ),
        alertas = if (odometro != null) calcularAlertas(manutencoes, odometro.km) else emptyList(),
        depreciacaoPorKmCentavos = depreciacaoPorKmCentavos,
        odometro = odometro,
        kmPessoais = kmPessoais.sortedWith(compareByDescending<KmPessoal> { it.data.toEpochDay() }.thenByDescending { it.id }),
        conferencia = conferencia,
    )
}

private data class Registros(
    val abastecimentos: List<Abastecimento>,
    val manutencoes: List<Manutencao>,
    val odometro: Odometro?,
    val kmPessoais: List<KmPessoal>,
    val conferencia: ConferenciaKm?,
)

class VeiculoViewModel(repo: Repositorio, configuracoes: Configuracoes) : ViewModel() {

    private val mes = YearMonth.now()

    private val registros = combine(
        repo.abastecimentos(),
        repo.manutencoes(),
        repo.odometro(),
        repo.kmPessoais(),
        repo.conferenciaKm(),
    ) { a, m, o, p, c ->
        Registros(a, m, o, p, c)
    }

    val uiState: StateFlow<VeiculoUiState> = combine(
        registros,
        repo.ganhos(mes.atDay(1), mes.atEndOfMonth()),
        repo.despesas(mes.atDay(1), mes.atEndOfMonth()),
        configuracoes.depreciacaoPorKmCentavos,
    ) { r, ganhos, despesas, depreciacao ->
        montarEstadoVeiculo(
            r.abastecimentos, r.manutencoes, ganhos, despesas, depreciacao, r.odometro, r.kmPessoais, r.conferencia,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VeiculoUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MotoristaApp
                VeiculoViewModel(app.repositorio, app.configuracoes)
            }
        }
    }
}
