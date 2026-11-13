package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import ar.jornadas.data.Charla
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios.deProceso(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn<List<Charla>?>(viewModelScope, SharingStarted.Eagerly, null)
    val descargada = repo.hayEdicion.stateIn<Boolean?>(viewModelScope, SharingStarted.Eagerly, null)
    val dia = estado.getStateFlow("dia", 0)
    val tema = estado.getStateFlow("tema", "Todos")
    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    private var pendiente = false

    fun elegirDia(valor: Int) { estado["dia"] = valor }
    fun elegirTema(valor: String) { estado["tema"] = valor }

    fun cargaInicial() {
        if (servicios.cargaInicial.compareAndSet(false, true)) recargar()
    }

    fun recargar() {
        if (cargando.value) { pendiente = true; return }
        cargando.value = true
        viewModelScope.launch {
            try {
                do {
                    pendiente = false
                    try {
                        repo.sincronizar()
                        aviso.value = ""
                    } catch (e: IOException) {
                        aviso.value = "No se pudo consultar el programa"
                    }
                } while (pendiente)
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
