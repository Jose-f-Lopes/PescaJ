import com.github.javaparser.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import javax.xml.stream.events.Comment
import pt.iscte.javardise.external.*


internal fun parserFromFile(path:String,fileManager:FileManager):Pair<List<Pckg>,CombinedTypeSolver>{
    val list= mutableListOf<Pckg>()

    val file=File(path)
    try {
        file.walk(FileWalkDirection.TOP_DOWN).filter { dir->dir.isDirectory }.forEach {
            val watcher = FileSystems.getDefault().newWatchService()
            it.toPath().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            val thread=Thread(FileWatcher(watcher,fileManager,it.absolutePath))
            thread.start()
            fileManager.registedThreads.add(thread)

        }

    }catch (e: IOException){
        println(e.message)
        throw e
    }
    file.walk(FileWalkDirection.TOP_DOWN).forEach {file->
        file.listFiles()?.let {
            if (it.filter { it.name.endsWith(".java") }.isNotEmpty()) {
                var name=(file.absolutePath.toString()).split("src\\").last()
                name=name.replace("\\",".")
                val newpckg=Pckg(file,name)
                if (newpckg.javaClasses.isNotEmpty()){
                    list.add(newpckg)
                }
            }
        }
    }

    println(list.size.toString()+" java files")
    return Pair(list,buildSolver(file))
}

private fun buildSolver(proj:File):CombinedTypeSolver{
    val solver = CombinedTypeSolver()
    solver.add(ReflectionTypeSolver())

    proj.walkTopDown().filter { (it.listFiles()?.filter { it.name.endsWith(".java") })?.isNotEmpty()?:false}.forEach {

        solver.add(JavaParserTypeSolver(it))
    }
    val symbolSolver = JavaSymbolSolver(solver)
    StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver)
    return solver
}

internal class FileWatcher(val watcher:WatchService,val fileManager: FileManager,val path:String): Runnable{
    override fun run() {
        while (true) {
            val key: WatchKey
            try {
                key = watcher.take()
            } catch (x: InterruptedException) {
                return
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()

                // This key is registered only
                // for ENTRY_CREATE events,
                // but an OVERFLOW event can
                // occur regardless if events
                // are lost or discarded.
                if (kind === OVERFLOW) {
                    continue
                }

                // The filename is the
                // context of the event.
                val ev = event as WatchEvent<Path>
                val filename = ev.context().toString()
                if(kind== ENTRY_CREATE){
                    if(filename.endsWith(".java")){
                        //fileManager.addJavaFile(path+"\\"+filename)
                    }
                }else if (kind== ENTRY_DELETE){
                    println(filename)
                    val file=File(path+"\\"+filename)
                }

            }
            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.

            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.
            val valid = key.reset()
            if (!valid) {
                break
            }
        }
    }
}

class Pckg(pckg:File,uptoHere:String){
    val documentation:File?
    val docComUnit:CompilationUnit?
    val javaClasses= mutableListOf<CompilationUnit>()
    val name:String
    val treeName=uptoHere

    init {
        pckg.listFiles()?.let {
            it.filter { it.name.endsWith(".java") }.forEach {
                //javaClasses.add(StaticJavaParser.parse(it))
                javaClasses.add(loadCompilationUnit(it))
            }
        }
        name=pckg.nameWithoutExtension
        println(treeName)
        documentation=pckg.listFiles()?.firstOrNull{it.name=="package-info.java"}
        if (documentation!=null){
            docComUnit=StaticJavaParser.parse(documentation)
        }else{
            docComUnit=null
        }
        //TODO
        //println(StaticJavaParser.parse(documentation).comment.getOrNull.toString())
    }

    fun getTypeDeclaration(): MutableList<TypeDeclaration<*>> {
        val temp= mutableListOf<TypeDeclaration<*>>()
        javaClasses.forEach {
            it.types.forEach {
                temp.add(it)
            }
        }
        return temp
    }

    fun getDocumentation():com.github.javaparser.ast.comments.Comment?{
        return docComUnit?.comment?.getOrNull
    }
}
