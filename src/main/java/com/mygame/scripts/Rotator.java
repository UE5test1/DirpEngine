import com.myengine.ecs.Component;
import org.joml.Vector3f;

public class Orbiter extends Component {
    public Vector3f centerPoint = new Vector3f(0, 0, 0);
    public float radius = 3f;
    public float speed = 1f; // full orbits per second
    
    private float angle = 0f;
    
    @Override
    public void update(float deltaTime) {
        angle += speed * 360f * deltaTime;
        float radians = (float) Math.toRadians(angle);
        gameObject.transform.position.x = centerPoint.x + (float) Math.cos(radians) * radius;
        gameObject.transform.position.z = centerPoint.z + (float) Math.sin(radians) * radius;
    }
}