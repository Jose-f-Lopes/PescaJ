//package point;
import cubes.Square;
public class Main {
    public static void main(String[] args) {
        Vector vector=new Vector(new Point(3,2));
        System.out.println(vector.getNormal().crossProduct(vector));
        Square square=new Square(new Point(0,0),new Point(2,2));
        for (Point p:square.getPoints()){
            System.out.println(p);
        }
        System.out.println(square.area());
    }
}