interface Command{
    /**
     * The behaviour of the command should be implemented here.
     */
    fun run()

    /**
     * The inverse behaviour of the command should be implemented here.
     */
    fun undo()
}

enum class EventType{
    PROJECTLOADED,
    FILEADDED
}

//internal class DuplicateMember(val parent:JavaParserWrapper,val child:String):Command{
//    override fun run() {
//        parent.addMember(child)
//    }
//
//    override fun undo() {
//        parent.removeMember(child)
//    }
//
//}