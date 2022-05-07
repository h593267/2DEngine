package no.arnemunthekaas.engine.entities.components;

import no.arnemunthekaas.utils.AssetPool;
import org.joml.Vector2f;

public class BlockCoin extends Component {
    private Vector2f topY;
    private float coinSpeed = 1.4f;

    @Override
    public void start() {
        topY = new Vector2f(this.gameObject.transform.position.y).add(0, 0.5f);
        AssetPool.getSound("assets/audio/assets_sounds_1-up.ogg").play();
    }

    @Override
    public void update(float dt) {
        if (this.gameObject.transform.position.y < topY.y) {
            this.gameObject.transform.position.y += dt * coinSpeed;
            this.gameObject.transform.scale.x -= (0.5f * dt) % -1.0f;
        } else {
            gameObject.destroy();
        }
    }
}
