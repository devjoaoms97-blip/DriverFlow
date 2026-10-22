package br.com.motoristafinanceiro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
        const val NOME = "motorista.db"

        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NOME)
                .addMigrations(*MIGRACOES)
                .build()
    }
}

/** v1 → v2: horário de início do turno nos ganhos (opcional; lançamentos antigos ficam sem). */
val MIGRACAO_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ganhos ADD COLUMN inicioTurnoMinutos INTEGER")
    }
}

/** Todas as migrações, em ordem. Nunca apague uma: o celular pode estar em qualquer versão antiga. */
val MIGRACOES = arrayOf(MIGRACAO_1_2)

