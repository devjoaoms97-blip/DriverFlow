package br.com.motoristafinanceiro.ui.home

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
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.ProgressoMeta
import br.com.motoristafinanceiro.domain.calcularProgressoMeta
import br.com.motoristafinanceiro.domain.fimDaSemana
import br.com.motoristafinanceiro.domain.inicioDaSemana
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.domain.calcularAlertas
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import br.com.motoristafinanceiro.domain.odometroAtual
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class HomeUiState(
    val hoje: Estatisticas = Estatisticas(),
    /** Semana atual, de segunda a domingo (inclui dias do mês anterior quando a semana vira o mês). */
    val semana: Estatisticas = Estatisticas(),
    /** Ex.: "12/10 a 18/10". */
    val rotuloSemana: String = "",
    val mes: Estatisticas = Estatisticas(),
    val metaCentavos: Long? = null,
    /** Progresso e "quanto falta por dia"; null quando não há meta definida para o mês. */
    val progressoMeta: ProgressoMeta? = null,
    /** Manutenções atrasadas ou próximas (as em dia não aparecem no Início). */
    val alertasUrgentes: List<AlertaManutencao> = emptyList(),
)

/** Regra pura do Início (sem Android), coberta por testes. */
internal fun montarEstadoHome(
    ganhosMes: List<Ganho>,
    despesasMes: List<Despesa>,
    meta: Meta?,
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    hoje: LocalDate = LocalDate.now(),
    depreciacaoPorKmCentavos: Long = 0,
    /** Lançamentos que cobrem a semana de [hoje]; podem vir de dois meses. Dias fora da semana são ignorados. */
    ganhosSemana: List<Ganho> = emptyList(),
    despesasSemana: List<Despesa> = emptyList(),
): HomeUiState {
    val odometro = odometroAtual(abastecimentos, manutencoes)
    val alertas = if (odometro == null) {
        emptyList()
    } else {
        calcularAlertas(manutencoes, odometro).filter { it.status != StatusAlerta.EM_DIA }
    }
    val estatisticasMes = calcularEstatisticas(ganhosMes, despesasMes, depreciacaoPorKmCentavos)
    val semanaDeHoje = inicioDaSemana(hoje)..fimDaSemana(hoje)
    val metaValida = meta?.valorAlvoLucroCentavos?.takeIf { it > 0 }
    return HomeUiState(
        hoje = calcularEstatisticas(
            ganhosMes.filter { it.data == hoje },
            despesasMes.filter { it.data == hoje },
            depreciacaoPorKmCentavos,
        ),
        semana = calcularEstatisticas(
            ganhosSemana.filter { it.data in semanaDeHoje },
            despesasSemana.filter { it.data in semanaDeHoje },
            depreciacaoPorKmCentavos,
        ),
        rotuloSemana = rotuloSemana(hoje),
        mes = estatisticasMes,
        metaCentavos = meta?.valorAlvoLucroCentavos,
        progressoMeta = metaValida?.let { calcularProgressoMeta(it, estatisticasMes.lucroLiquidoCentavos, hoje) },
        alertasUrgentes = alertas,
    )
}

private data class DadosHome(
    val ganhos: List<Ganho>,
    val despesas: List<Despesa>,
    val meta: Meta?,
    val abastecimentos: List<Abastecimento>,
    val manutencoes: List<Manutencao>,
)

private data class DadosSemana(val ganhos: List<Ganho>, val despesas: List<Despesa>)

class HomeViewModel(repo: Repositorio, configuracoes: Configuracoes) : ViewModel() {

    private val mesAtual = YearMonth.now()
    private val inicioSemana = inicioDaSemana(LocalDate.now())
    private val fimSemana = fimDaSemana(LocalDate.now())

    private val semana = combine(
        repo.ganhos(inicioSemana, fimSemana),
        repo.despesas(inicioSemana, fimSemana),
    ) { ganhos, despesas -> DadosSemana(ganhos, despesas) }

    private val dados = combine(
        repo.ganhos(mesAtual.atDay(1), mesAtual.atEndOfMonth()),
        repo.despesas(mesAtual.atDay(1), mesAtual.atEndOfMonth()),
        repo.meta(mesAtual.toString()),
        repo.abastecimentos(),
        repo.manutencoes(),
    ) { ganhos, despesas, meta, abastecimentos, manutencoes ->
        DadosHome(ganhos, despesas, meta, abastecimentos, manutencoes)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        dados,
        semana,
        configuracoes.depreciacaoPorKmCentavos,
    ) { d, s, depreciacao ->
        montarEstadoHome(
            d.ganhos, d.despesas, d.meta, d.abastecimentos, d.manutencoes,
            depreciacaoPorKmCentavos = depreciacao,
            ganhosSemana = s.ganhos,
            despesasSemana = s.despesas,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MotoristaApp
                HomeViewModel(app.repositorio, app.configuracoes)
            }
        }
    }
}
