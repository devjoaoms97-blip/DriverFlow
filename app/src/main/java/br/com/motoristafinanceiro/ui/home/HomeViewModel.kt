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
import br.com.motoristafinanceiro.data.model.KmPessoal
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.MetaDoDia
import br.com.motoristafinanceiro.domain.Odometro
import br.com.motoristafinanceiro.domain.ProgressoMeta
import br.com.motoristafinanceiro.domain.calcularMetaDoDia
import br.com.motoristafinanceiro.domain.calcularProgressoMeta
import br.com.motoristafinanceiro.domain.metaParaRepetirCentavos
import br.com.motoristafinanceiro.domain.fimDaSemana
import br.com.motoristafinanceiro.domain.inicioDaSemana
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.domain.calcularAlertas
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import br.com.motoristafinanceiro.domain.odometroAtual
import br.com.motoristafinanceiro.ui.reservas.ReservasUiState
import br.com.motoristafinanceiro.ui.reservas.montarEstadoReservas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

data class HomeUiState(
    val hoje: Estatisticas = Estatisticas(),
    /** Semana atual, de segunda a domingo (inclui dias do mês anterior quando a semana vira o mês). */
    val semana: Estatisticas = Estatisticas(),
    /** Ex.: "12/10 a 18/10". */
    val rotuloSemana: String = "",
    val mes: Estatisticas = Estatisticas(),
    val metaCentavos: Long? = null,
    /** Meta do mês anterior que o card oferece repetir; null se o mês já tem meta ou o anterior não tinha. */
    val metaParaRepetirCentavos: Long? = null,
    /** Meta de hoje tirada da do mês; null sem meta ou com o mês já batido até ontem. */
    val metaDoDia: MetaDoDia? = null,
    /** Progresso e "quanto falta por dia"; null quando não há meta definida para o mês. */
    val progressoMeta: ProgressoMeta? = null,
    /** Manutenções atrasadas ou próximas (as em dia não aparecem no Início). */
    val alertasUrgentes: List<AlertaManutencao> = emptyList(),
    /** Despesas fixas: quanto separar por dia e o próximo vencimento (atalho para a tela Reservas). */
    val reservas: ReservasUiState = ReservasUiState(),
    /** Aviso de backup velho ou nunca feito; null quando está em dia. */
    val avisoBackup: TextoAviso? = null,
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
    reservas: List<ReservaDespesa> = emptyList(),
    /** Odômetro com os km dos ganhos (estimado); null usa só as leituras de abastecimentos e manutenções. */
    odometroEstimado: Odometro? = null,
    metaMesAnterior: Meta? = null,
    /** Dia do último backup (manual ou automático); null = nunca. */
    ultimoBackup: LocalDate? = null,
    /** Km pessoais que cobrem o mês e a semana de [hoje]; cada período filtra os seus. */
    kmPessoais: List<KmPessoal> = emptyList(),
): HomeUiState {
    val odometro = odometroEstimado?.km ?: odometroAtual(abastecimentos, manutencoes)
    val alertas = if (odometro == null) {
        emptyList()
    } else {
        calcularAlertas(manutencoes, odometro).filter { it.status != StatusAlerta.EM_DIA }
    }
    val semanaDeHoje = inicioDaSemana(hoje)..fimDaSemana(hoje)
    val mesDeHoje = YearMonth.from(hoje)
    val estatisticasMes = calcularEstatisticas(
        ganhosMes, despesasMes, depreciacaoPorKmCentavos, kmPessoais.filter { YearMonth.from(it.data) == mesDeHoje },
    )
    val metaValida = meta?.valorAlvoLucroCentavos?.takeIf { it > 0 }
    val estatisticasHoje = calcularEstatisticas(
        ganhosMes.filter { it.data == hoje },
        despesasMes.filter { it.data == hoje },
        depreciacaoPorKmCentavos,
        kmPessoais.filter { it.data == hoje },
    )
    return HomeUiState(
        hoje = estatisticasHoje,
        semana = calcularEstatisticas(
            ganhosSemana.filter { it.data in semanaDeHoje },
            despesasSemana.filter { it.data in semanaDeHoje },
            depreciacaoPorKmCentavos,
            kmPessoais.filter { it.data in semanaDeHoje },
        ),
        rotuloSemana = rotuloSemana(hoje),
        mes = estatisticasMes,
        metaCentavos = meta?.valorAlvoLucroCentavos,
        metaParaRepetirCentavos = metaParaRepetirCentavos(meta?.valorAlvoLucroCentavos, metaMesAnterior?.valorAlvoLucroCentavos),
        metaDoDia = metaValida?.let {
            calcularMetaDoDia(it, estatisticasMes.lucroLiquidoCentavos, estatisticasHoje.lucroLiquidoCentavos, hoje)
        },
        progressoMeta = metaValida?.let { calcularProgressoMeta(it, estatisticasMes.lucroLiquidoCentavos, hoje) },
        alertasUrgentes = alertas,
        reservas = montarEstadoReservas(reservas, hoje),
        avisoBackup = textoAvisoBackup(
            ultimoBackup,
            hoje,
            temDados = ganhosMes.isNotEmpty() || despesasMes.isNotEmpty() || abastecimentos.isNotEmpty() ||
                manutencoes.isNotEmpty() || reservas.isNotEmpty(),
        ),
    )
}

private data class DadosHome(
    val ganhos: List<Ganho>,
    val despesas: List<Despesa>,
    val meta: Meta?,
    val abastecimentos: List<Abastecimento>,
    val manutencoes: List<Manutencao>,
)

private data class DadosSemana(
    val ganhos: List<Ganho>,
    val despesas: List<Despesa>,
    val reservas: List<ReservaDespesa>,
    val odometro: Odometro?,
    val metaAnterior: Meta?,
)

class HomeViewModel(private val repo: Repositorio, configuracoes: Configuracoes) : ViewModel() {

    val mesAtual: YearMonth = YearMonth.now()
    private val inicioSemana = inicioDaSemana(LocalDate.now())
    private val fimSemana = fimDaSemana(LocalDate.now())

    private val semana = combine(
        repo.ganhos(inicioSemana, fimSemana),
        repo.despesas(inicioSemana, fimSemana),
        repo.reservas(),
        repo.odometro(),
        repo.meta(mesAtual.minusMonths(1).toString()),
    ) { ganhos, despesas, reservas, odometro, metaAnterior ->
        DadosSemana(ganhos, despesas, reservas, odometro, metaAnterior)
    }

    private val dados = combine(
        repo.ganhos(mesAtual.atDay(1), mesAtual.atEndOfMonth()),
        repo.despesas(mesAtual.atDay(1), mesAtual.atEndOfMonth()),
        repo.meta(mesAtual.toString()),
        repo.abastecimentos(),
        repo.manutencoes(),
    ) { ganhos, despesas, meta, abastecimentos, manutencoes ->
        DadosHome(ganhos, despesas, meta, abastecimentos, manutencoes)
    }

    /** Km pessoais do mês e da semana (a semana pode começar no mês anterior ou terminar no seguinte). */
    private val kmPessoais = repo.kmPessoais(
        minOf(inicioSemana, mesAtual.atDay(1)),
        maxOf(fimSemana, mesAtual.atEndOfMonth()),
    )

    val uiState: StateFlow<HomeUiState> = combine(
        dados,
        semana,
        configuracoes.depreciacaoPorKmCentavos,
        configuracoes.ultimoBackupMs,
        kmPessoais,
    ) { d, s, depreciacao, ultimoBackupMs, pessoais ->
        montarEstadoHome(
            d.ganhos, d.despesas, d.meta, d.abastecimentos, d.manutencoes,
            depreciacaoPorKmCentavos = depreciacao,
            ganhosSemana = s.ganhos,
            despesasSemana = s.despesas,
            reservas = s.reservas,
            odometroEstimado = s.odometro,
            metaMesAnterior = s.metaAnterior,
            ultimoBackup = ultimoBackupMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
            kmPessoais = pessoais,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Grava como meta deste mês o valor da meta do mês anterior (o card do Início oferece com um toque). */
    fun repetirMetaAnterior(centavos: Long) {
        viewModelScope.launch { repo.salvarMeta(Meta(mesAtual.toString(), centavos)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MotoristaApp
                HomeViewModel(app.repositorio, app.configuracoes)
            }
        }
    }
}
