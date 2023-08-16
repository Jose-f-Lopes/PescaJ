//package point;

public class Vector {

    private Point a=new Point(0,0);
    private Point b;

    public Vector(Point b){
        this.b=b;
    }

    public Point getPoint() {
        return b;
    }
    public double magnitude(){
        return a.distanceToPoint(b);
    }
    public Vector getNormal(){
        return new Vector(new Point(-(getPoint().getY()/getPoint().getX()),1));
    }

    public double crossProduct(Vector vector){
        return (getPoint().getY()*vector.getPoint().getY())+(getPoint().getX()*vector.getPoint().getX());
    }

    @Override
    public String toString(){
        return getPoint().getX()+","+ getPoint().getY();
    }

}
