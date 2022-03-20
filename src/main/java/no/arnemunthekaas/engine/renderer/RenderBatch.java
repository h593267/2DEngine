package no.arnemunthekaas.engine.renderer;

import no.arnemunthekaas.engine.Window;
import no.arnemunthekaas.engine.entities.components.SpriteRenderer;
import no.arnemunthekaas.engine.utils.AssetPool;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;


public class RenderBatch {
    /*
    Vertex
    ======
    Pos                         Color                           Texture Coordinates         Texture ID
    float, float,               float, float, float, float,      float, float,              float
     */

    private final int POS_SIZE = 2;
    private final int COLOR_SIZE = 4;
    private final int TEX_COORDS_SIZE = 2;
    private final int TEX_ID_SIZE = 1;

    private final int POS_OFFSET = 0;
    private final int COLOR_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
    private final int TEX_COORDS_OFFSET = COLOR_OFFSET + COLOR_SIZE * Float.BYTES;
    private final int TEX_ID_OFFSET = TEX_COORDS_OFFSET + TEX_COORDS_SIZE * Float.BYTES;
    private final int VERTEX_SIZE = 9;
    private final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private SpriteRenderer[] sprites;
    private int spriteAmount;
    private boolean hasSpace;
    private float[] vertices;
    private int[] texSlots = {0, 1, 2, 3, 4, 5, 6, 7};

    private List<Texture> textures;
    private int vaoID, vboID;
    private int maxBatchSize;
    private Shader shader;

    /**
     * Creates new Render Batch with max amount of quads (sprites) to hold. Bigger = better performance but longer loading
     * @param maxBatchSize Max quad (sprite) amount
     */
    public RenderBatch(int maxBatchSize) {
        shader = AssetPool.getShader("assets/shaders/default.glsl");
        this.sprites = new SpriteRenderer[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        // 4 vertices quads
        vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];

        this.spriteAmount = 0;
        this.hasSpace = true;
        this.textures = new ArrayList<>();
    }

    /**
     * Create data on GPU, allocate space on GPU
     */
    public void start() {
        // Generate and bind a Vertex Array Object
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Allocate space for vertices
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        // Create and upload indices buffer
        int eboID = glGenBuffers();
        int[] indices = generateIndices();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Enable the buffer attribute pointers
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_OFFSET);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TEX_COORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_COORDS_OFFSET);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_ID_OFFSET);
        glEnableVertexAttribArray(3);
    }

    /**
     * Add sprite to Render Batch
     * @param spriteRenderer SpriteRenderer
     */
    public void addSprite(SpriteRenderer spriteRenderer) {
        // Get index and add renderObject
        int index = this.spriteAmount;
        this.sprites[index] = spriteRenderer;
        this.spriteAmount++;

        if(spriteRenderer.getTexture() != null)
            if(!textures.contains(spriteRenderer.getTexture()))
                textures.add(spriteRenderer.getTexture());

        // Add properties to local vertices array
        loadVertexProperties(index);

        if (spriteAmount >= this.maxBatchSize)
            this.hasSpace = false;

    }

    /**
     * Render (and rebuffer data)
     */
    public void render() {

        boolean rebufferData = false;
        for (int i = 0; i < spriteAmount; i++) {
            SpriteRenderer spr = sprites[i];
            if (spr.isDirty()){
                loadVertexProperties(i);
                spr.setClean();
                rebufferData = true;
            }
        }

        // If any sprites were dirty -> rebuffer data
        if (rebufferData) {
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        }

        // Use shader
        shader.use();
        shader.uploadMat4f("uProjection", Window.getScene().getCamera().getProjectionMat());
        shader.uploadMat4f("uView", Window.getScene().getCamera().getViewMat());

        // Bind textures
        for(int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            textures.get(i).bind();
        }
        shader.uploadIntArray("uTextures", texSlots);

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawElements(GL_TRIANGLES, this.spriteAmount * 6, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        // Unbind textures
        for (Texture texture : textures) texture.unbind();


        shader.detach();
    }

    private void loadVertexProperties(int index) {
        SpriteRenderer sprite = this.sprites[index];

        // Find offset within array (4 vertices per sprite)
        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = sprite.getColor();
        Vector2f[] texCoords = sprite.getTextureCoordinates();

        int texID = 0;
        if(sprite.getTexture()!= null)
            for (int i = 0; i < textures.size(); i++) {
                if(textures.get(i) == sprite.getTexture()) {
                    texID = i + 1;
                    break;
                }
            }

        // Add vertices with the appropriate properties
        float xAdd = 1.0f;
        float yAdd = 1.0f;
        for (int i=0; i < 4; i++) {
            if (i == 1) {
                yAdd = 0.0f;
            } else if (i == 2) {
                xAdd = 0.0f;
            } else if (i == 3) {
                yAdd = 1.0f;
            }

            // Load position
            vertices[offset] = sprite.gameObject.transform.position.x + (xAdd * sprite.gameObject.transform.scale.x);
            vertices[offset+1] = sprite.gameObject.transform.position.y + (yAdd * sprite.gameObject.transform.scale.y);

            // Load color
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;

            // Load texture coordinates
            vertices[offset + 6] = texCoords[i].x;
            vertices[offset + 7] = texCoords[i].y;

            // Load texture ID
            vertices[offset + 8] = texID;

            offset += VERTEX_SIZE;

        }
    }

    private int[] generateIndices() {
        // 6 indices per quad (3 per triangle)
        int[] elements = new int[6*maxBatchSize];
        for (int i = 0; i < maxBatchSize; i++) {
            loadElementIndices(elements, i);
        }

        return elements;
    }

    private void loadElementIndices(int[] elements, int index) {
        int offsetArrayIndex = 6 * index;
        int offset = 4 * index;

        // Triangle 1
        elements[offsetArrayIndex] = offset + 3;
        elements[offsetArrayIndex + 1] = offset + 2;
        elements[offsetArrayIndex + 2] = offset + 0;

        // Triangle 2
        elements[offsetArrayIndex + 3] = offset + 0;
        elements[offsetArrayIndex + 4] = offset + 2;
        elements[offsetArrayIndex + 5] = offset + 1;
    }

    /**
     * Returns if batch has more space
     *
     * @return True if batch has more space
     */
    public boolean hasSpace() {
        return hasSpace;
    }

    /**
     * Check if there is space for more textures
     * @return True/False
     */
    public boolean hasTextureSpace() {
        return this.textures.size() < 8;
    }

    /**
     * Check if batch contains texture
     * @param tex Texture to check for
     * @return True if contains texture
     */
    public boolean containsTexture(Texture tex) {
        return textures.contains(tex);
    }
}
