import com.github.javaparser.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WorkSpaceModelTest{
    companion object{

        private val manager=FileManager()
        private const val testProjectPath="C:\\Users\\jose1\\Desktop\\QuintalDJ\\src\\test\\kotlin\\JavaParserTestingFacility"
        lateinit var solver: CombinedTypeSolver
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
    fun workSpaceLogicTest(){

        val root= javaFiles.first { it.primaryTypeName.get()== "JavaCode2" }.getClassByName("JavaCode2").get().methods.first{it.nameAsString=="increment"}
        val containedSameClass= javaFiles.first { it.primaryTypeName.get()== "JavaCode2" }.getClassByName("JavaCode2").get().methods.first{it.nameAsString=="saySomething"}
        val containedDifferentClass= javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first{it.nameAsString=="increment"}
        val notContainedSameClass= javaFiles.first { it.primaryTypeName.get()== "JavaCode2" }.getClassByName("JavaCode2").get().methods.first{it.nameAsString=="anotherMethod"}
        val notContainedDifferentClass= javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first{it.nameAsString=="saySomething"}

        val depth2=javaFiles.first {it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first { it.nameAsString=="sum"}
        val depth3=javaFiles.first {it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first { it.nameAsString=="convert"}

        val workSpace=WorkSpaceModel()
        workSpace.addSolver(solver)

        workSpace.addMethod(root)
        workSpace.addMethod(containedSameClass)
        workSpace.addMethod(containedDifferentClass)
        workSpace.addMethod(notContainedSameClass)
        workSpace.addMethod(notContainedDifferentClass)

        workSpace.addMethod(depth2)
        workSpace.addMethod(depth3)

        //println(workSpace.mytoString())

        val (bool1,class1)=workSpace.hasClass( javaFiles.first { it.primaryTypeName.get()== "JavaCode2" }.getClassByName("JavaCode2").get())
        val (bool2,class2)=workSpace.hasClass( javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get())

        assertEquals(true,bool1)
        assertEquals(true,bool2)

        assertEquals(1,workSpace.depthOf(class1!!))
        assertEquals(2,workSpace.depthOf(class2!!))
        assertNotEquals(3,workSpace.depthOf(class2))

        val (bool3,method1)=workSpace.hasMethod(javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first { it.nameAsString=="increment"})
        val (bool4,method2)=workSpace.hasMethod(javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first { it.nameAsString=="sum"})
        val (bool5,method3)=workSpace.hasMethod(javaFiles.first { it.primaryTypeName.get()== "JavaCode" }.getClassByName("JavaCode").get().methods.first { it.nameAsString=="convert"})

        assertTrue(bool3)
        assertTrue(bool4)
        assertTrue(bool5)

        assertEquals(1,method1!!.inClassDepth())
        assertEquals(2,method2!!.inClassDepth())
        assertEquals(3,method3!!.inClassDepth())
    }

}

