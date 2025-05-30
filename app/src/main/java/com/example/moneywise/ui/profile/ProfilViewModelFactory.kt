import com.example.moneywise.ui.profile.ProfilViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.data.repository.UtilisateurRepository

class ProfilViewModelFactory(
    private val utilisateurRepository: UtilisateurRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfilViewModel::class.java)) {
            return ProfilViewModel(utilisateurRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}