import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.TypeDeclaration
import java.io.File

class FileManager() :IObservable<(EventType, FileManager, CompilationUnit?) -> Unit> {


    override val observers: MutableList<(EventType, FileManager, CompilationUnit?) -> Unit> = mutableListOf()
    var javaFiles= mutableListOf<CompilationUnit>()
    val registedThreads= mutableListOf<Thread>()

    init {

    }
    public fun getFunList():List<String>{
       val temp= mutableListOf<String>()
        try {
            javaFiles.forEach { it.getClassByName(it.primaryTypeName.get()).get().methods.forEach { temp.add(it.declarationAsString) } }
        }catch (e:NoSuchElementException){

        }
        return temp
    }

    public fun setDirectory(path: String){
        stopThreads()
        val javaFiles=parserFromFile(path,this)
        if(javaFiles.size==0){
            throw IllegalArgumentException("not a java project :(")
        }else{
            this.javaFiles.addAll(javaFiles)
        }
        notifyObservers { it(EventType.PROJECTLOADED,this,null) }
    }

    fun addJavaFile(path:String){
        println("adding")
        val fileparser=StaticJavaParser.parse(File(path))
        javaFiles.add(fileparser)
        notifyObservers { it(EventType.FILEADDED,this,fileparser) }
    }

    public fun stopThreads(){
        for(thread in registedThreads){
            thread.interrupt()
        }
    }

    fun getTypeDeclaration(): MutableList<TypeDeclaration<*>> {
        val temp= mutableListOf<TypeDeclaration<*>>()
        javaFiles.forEach { it.types.forEach {temp.add( it) } }
        return temp

    }

}