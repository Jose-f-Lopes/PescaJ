import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.Observable
import com.github.javaparser.ast.observer.ObservableProperty
import pt.iscte.javardise.external.PropertyObserver

fun <T> Observable.observeProperty(prop: ObservableProperty, event: (T?) -> Unit): AstObserver {
    val obs = object : PropertyObserver<T>(prop) {
        override fun modified(oldValue: T?, newValue: T?) {
            event(newValue)
        }
    }
    register(obs)
    return obs
}