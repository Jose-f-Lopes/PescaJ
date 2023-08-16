package cubes;

import point.Point;

public class Square {

    private Point a;
    private Point b;
    private Point c;
    private Point d;

    public Square(Point bottomLeft, Point topRight){
        if(bottomLeft.getX()>=topRight.getX() || bottomLeft.getY()>=topRight.getY()){
            throw new IllegalArgumentException("invalid square");
        }
        this.a=bottomLeft;
        this.b=new Point(topRight.getX(),bottomLeft.getY());
        this.c=topRight;
        this.d=new Point(bottomLeft.getX(),topRight.getY());
    }

    public Point[] getPoints(){
        return new Point[]{a, b, c, d};
    }

    public double perimeter(){
        return  a.distanceToPoint(b)+b.distanceToPoint(c)+c.distanceToPoint(d)+d.distanceToPoint(a);
    }

    public double area(){
        return a.distanceToPoint(b)*b.distanceToPoint(c);
    }
	
	private double diagonal(){
		return a.distanceToPoint(c);
	}
	
	protected double height(){
		return a.distanceToPoint(b);
	}
}
