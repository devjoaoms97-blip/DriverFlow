package br.com.motoristafinanceiro.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

class Conversores {
    @TypeConverter
    fun dataParaLong(data: LocalDate?): Long? = data?.toEpochDay()

    @TypeConverter
    fun longParaData(valor: Long?): LocalDate? = valor?.let { LocalDate.ofEpochDay(it) }
}
