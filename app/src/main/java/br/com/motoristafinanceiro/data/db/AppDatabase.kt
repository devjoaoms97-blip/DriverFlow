package br.com.motoristafinanceiro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.ReservaDespesa

@Database(
    entities = [
        Ganho::class,
        Despesa::class,
        Abastecimento::class,
        Manutencao::class,
        ReservaDespesa::class,
        Meta::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ganhoDao(): GanhoDao
    abstract fun despesaDao(): DespesaDao
    abstract fun abastecimentoDao(): AbastecimentoDao
    abstract fun manutencaoDao(): ManutencaoDao
    abstract fun reservaDao(): ReservaDao
    abstract fun metaDao(): MetaDao

    companion object {
        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "motorista.db")
                .build()
    }
}
