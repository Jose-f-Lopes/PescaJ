import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class javaParserUtilTest{

    companion object{

        val manager=FileManager()
        val testProjectPath="C:\\Users\\jose1\\Desktop\\QuintalDJ\\src\\test\\kotlin\\JavaParserTestingFacility"
        lateinit var solver:CombinedTypeSolver
        lateinit var javaFiles:List<CompilationUnit>

        @BeforeAll
        @JvmStatic
        fun loadProject(){
            val (files,solver)=parserFromFile(testProjectPath,manager)
            this.solver=solver
            javaFiles=files
        }

        @AfterAll
        @JvmStatic
        fun unloadProject(){
            manager.stopThreads()
        }
    }

    @Test
    fun containsTest(){

        val target= javaFiles.first { it.primaryTypeName.get()== "JavaCode2" }.getClassByName("JavaCode2").get().methods.first{it.nameAsString=="increment"}
        val contains1= javaFiles.first { it.primaryTypeName.get()== "JavaCode2" }.getClassByName("JavaCode2").get().methods.first{it.nameAsString=="saySomething"}
        val contains2= javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first{it.nameAsString=="increment"}
        val doesNotContain= javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first{it.nameAsString=="saySomething"}

        assertEquals(true,javaParserUtil.contains(target,contains1, solver))
        assertEquals(true,javaParserUtil.contains(target,contains2, solver))
        assertEquals(false,javaParserUtil.contains(target,doesNotContain, solver))
    }



}
