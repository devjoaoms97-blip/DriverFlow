package br.com.motoristafinanceiro

import android.app.Application
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.db.AppDatabase

class MotoristaApp : Application() {
    val database by lazy { AppDatabase.criar(this) }
    val repositorio by lazy { Repositorio(database) }
    val configuracoes by lazy { Configuracoes.criar(this) }
}
