package otus.homework.reactivecats

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class CatsViewModel(
    val catsService: CatsService,
    val localCatFactsGenerator: LocalCatFactsGenerator,
    context: Context
) : ViewModel() {

    val catsSubject: PublishSubject<Result> = PublishSubject.create()
    private var compositeDisposable = CompositeDisposable()

    init {
        getFacts()
    }

    private fun getFacts() {
        compositeDisposable.addAll(
            Observable.interval(2000, TimeUnit.MILLISECONDS, Schedulers.io())
                .map { catsService.getCatFact() }
                .onErrorReturnItem(localCatFactsGenerator.generateCatFact().toObservable())
                .subscribe { factObservable ->
                    compositeDisposable.addAll(
                        factObservable
                            .onErrorResumeNext(
                                localCatFactsGenerator.generateCatFact().toObservable()
                            )
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { fact -> catsSubject.onNext(Success(fact)) },
                                { e ->
                                    Log.e("", "Error", e)
                                    when (e) {
                                        is HttpException -> catsSubject.onNext(ServerError)
                                        else -> catsSubject.onNext(Error(e.message ?: "Error"))
                                    }
                                }
                            ))
                })
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}

class CatsViewModelFactory(
    private val catsRepository: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    private val context: Context
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatsViewModel(catsRepository, localCatFactsGenerator, context) as T
}

sealed class Result
data class Success(val fact: Fact) : Result()
data class Error(val message: String) : Result()
object ServerError : Result()