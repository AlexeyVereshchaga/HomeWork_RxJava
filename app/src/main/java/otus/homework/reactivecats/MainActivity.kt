package otus.homework.reactivecats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {

    private val diContainer = DiContainer()
    private val catsViewModel by viewModels<CatsViewModel> {
        CatsViewModelFactory(
            diContainer.service,
            diContainer.localCatFactsGenerator(applicationContext),
            applicationContext
        )
    }

    private var disposable:Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_main, null) as CatsView
        setContentView(view)
        disposable = catsViewModel.catsSubject.subscribe { result ->
           when (result) {
               is Success -> view.populate(result.fact)
               is Error -> Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
               ServerError -> Snackbar.make(view, "Network error", 1000).show()
           }
       }
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }
}