package br.com.motoristafinanceiro.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// Convenção: todo valor em dinheiro é guardado em CENTAVOS (Long) para evitar erro de ponto flutuante.

/** Um lançamento de ganho por turno/dia e por plataforma. */
@Entity(tableName = "ganhos", indices = [Index("data")])
data class Ganho(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: LocalDate,
    val plataforma: Plataforma,
    val valorCentavos: Long,
    val gorjetaBonusCentavos: Long = 0,
    val km: Double = 0.0,
    val minutosOnline: Int = 0,
    val observacao: String? = null,
) {
    val totalCentavos: Long get() = valorCentavos + gorjetaBonusCentavos
}

@Entity(tableName = "despesas", indices = [Index("data"), Index("origem", "origemId")])
data class Despesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: LocalDate,
    val categoria: CategoriaDespesa,
    val valorCentavos: Long,
    val observacao: String? = null,
    val fotoUri: String? = null,
    val origem: OrigemDespesa = OrigemDespesa.MANUAL,
    val origemId: Long? = null,
)

@Entity(tableName = "abastecimentos", indices = [Index("odometroKm")])
data class Abastecimento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: LocalDate,
    val litros: Double,
    val valorTotalCentavos: Long,
    val odometroKm: Int,
    val tanqueCheio: Boolean = true,
)

@Entity(tableName = "manutencoes", indices = [Index("odometroKm")])
data class Manutencao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoManutencao,
    val data: LocalDate,
    val odometroKm: Int,
    val valorCentavos: Long,
    /** Intervalo em km até a próxima troca/revisão (null = sem alerta). */
    val intervaloKm: Int? = null,
    val observacao: String? = null,
)

/** Despesa recorrente/anual (IPVA, seguro, parcela do carro...) usada para calcular a reserva diária. */
@Entity(tableName = "reservas")
data class ReservaDespesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val valorCentavos: Long,
    val periodicidade: Periodicidade,
    val proximoVencimento: LocalDate,
)

/** Meta de lucro líquido por mês. [mes] no formato "2026-10". */
@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey val mes: String,
    val valorAlvoLucroCentavos: Long,
)
