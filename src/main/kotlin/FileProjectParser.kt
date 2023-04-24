import com.github.javaparser.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*


fun parserFromFile(path:String,fileManager:FileManager):Pair<List<CompilationUnit>,CombinedTypeSolver>{
    val list= mutableListOf<CompilationUnit>()

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
    file.walk(FileWalkDirection.TOP_DOWN).filter { it.path.endsWith(".java") }.forEach { list.add(StaticJavaParser.parse(it)) }
    println(list.size.toString()+" java files")
    return Pair(list,buildSolver(file))
}

private fun buildSolver(proj:File):CombinedTypeSolver{
    val solver = CombinedTypeSolver()
    solver.add(ReflectionTypeSolver())
    proj.walkTopDown().filter { (it.listFiles()?.filter { it.name.endsWith(".java") })?.isNotEmpty()?:false}.forEach {
        solver.add(JavaParserTypeSolver(it))
    }
    return solver
}

class FileWatcher(val watcher:WatchService,val fileManager: FileManager,val path:String): Runnable{
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
                        fileManager.addJavaFile(path+"\\"+filename)
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
