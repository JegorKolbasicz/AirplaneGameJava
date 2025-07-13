import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// Wyliczenie określające właściciela pocisku
enum BulletOwner {
    PLAYER,
    ENEMY
}

// Wyliczenie dla typów pocisków wroga
enum EnemyBulletType {
    TYPE1,
    TYPE2,
    TYPE3,
    TYPE4
}


abstract class Bullet {
    protected Plansza p;
    protected float x, y;
    protected float dx, dy;
    protected BulletOwner owner;
    protected Image image;
    protected Shape hitbox;
    protected int width, height;

    public Bullet(Plansza p, float startX, float startY, float dx, float dy, BulletOwner owner, Image image) {
        this.p = p;
        this.x = startX;
        this.y = startY;
        this.dx = dx;
        this.dy = dy;
        this.owner = owner;
        this.image = image;
        // Szerokość/wysokość są inicjalizowane już w klasach pochodnych.
        // Tutaj tylko wywołujemy metodę inicjalizacji hitboxa
        initializeHitbox();

        if (owner == BulletOwner.ENEMY && this.image instanceof BufferedImage) {
            this.image = rotateImage180((BufferedImage) this.image);
        }        
        
    }

    // Każda podklasa pocisku ma swój sposób inicjalizacji hitboxa
    protected abstract void initializeHitbox();

    // Aktualizacja hitboxa podczas ruchu
    protected abstract void updateHitbox();

    // Sprawdzanie kolizji
    protected abstract void checkCollision();


    

    // Logika ruchu pocisku
    public void move() {
        x += dx;
        y += dy;
        updateHitbox();
        checkCollision();

        // Jeśli pocisk wyleciał poza ekran - usuwamy go
        if (y < 0 || y > p.getHeight() || x < 0 - width || x > p.getWidth() + width) {
            p.removeBullet(this);
        }
    }

    // Rysowanie pocisku
    public void draw(Graphics2D g2d) {
        g2d.drawImage(image, (int)x, (int)y, width, height, null);
    }

    private BufferedImage rotateImage180(BufferedImage src) {
        AffineTransform transform = new AffineTransform();
        // Obrót o 180 stopni wokół środka obrazu
        transform.rotate(Math.toRadians(180), src.getWidth() / 2.0, src.getHeight() / 2.0);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage rotated = op.filter(src, null);
        return rotated;
    }

}


class Explosion {
    private float x, y;
    
    private List<Image> frames; 
    
    private Image gif;
    
    private int currentFrame;
    private long lastFrameTime;
    private int frameDelay;
    private boolean finished;

    public Explosion(float x, float y, List<Image> frames, int frameDelay) {
        this.x = x;
        this.y = y;
        this.frames = frames;
        this.frameDelay = frameDelay;
        this.currentFrame = 0;
        this.lastFrameTime = System.currentTimeMillis();
        this.finished = false;
    }

    public Explosion(float x, float y, Image gif) {
        this.x = x;
        this.y = y;
        this.gif = gif;
        this.finished = false;
        this.frames = null;
    }

    public void update() {
        if (finished) return;

        if (frames != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime >= frameDelay) {
                currentFrame++;
                lastFrameTime = currentTime;
                if (currentFrame >= frames.size()) {
                    finished = true;
                }
            }
        }
        else {
            long currentTime = System.currentTimeMillis();
            if (lastFrameTime == 0) {
                lastFrameTime = currentTime;
            }
            if (currentTime - lastFrameTime > 400) { 
                finished = true;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        if (finished) return;
    
        int desiredWidth = 100;  
        int desiredHeight = 100; 
    
        if (frames != null) {
            Image currentImage = frames.get(currentFrame);
            g2d.drawImage(currentImage, (int)x, (int)y, desiredWidth, desiredHeight, null);
        } else {
            g2d.drawImage(gif, (int)x, (int)y, desiredWidth, desiredHeight, null);
        }
    }
    

    public boolean isFinished() {
        return finished;
    }

    public Rectangle2D getBounds() {
        if (finished) return null;
        
        if (frames != null) {
            Image currentImage = frames.get(currentFrame);
            return new Rectangle2D.Float(x, y, 
                      currentImage.getWidth(null), 
                      currentImage.getHeight(null));
        } else {
            // Dla GIF 
            if (gif == null) return null;
            return new Rectangle2D.Float(x, y, 
                      gif.getWidth(null), 
                      gif.getHeight(null));
        }
    }
}


class ExplosionLoader {
    public static List<Image> loadExplosionFrames(String basePath, int frameCount) {
        List<Image> frames = new ArrayList<>();
        for (int i = 1; i <= frameCount; i++) {
            String path = basePath + "explosion" + i + ".png";
            try {
                Image img = ImageIO.read(ExplosionLoader.class.getResource(path));
                frames.add(img);
            } catch (IOException | IllegalArgumentException e) {
                System.err.println("Nie udało się załadować klatki wybuchu: " + path);
                e.printStackTrace();
            }
        }
        return frames;
    }
}

// Klasa reprezentująca ulepszenie
class PowerUp {
    float x, y;
    int width, height;
    Image image;
    PowerUpType type;
    float dy = 2; // Prędkość spadania

    enum PowerUpType {
        EXTRA_LIFE,
        INCREASE_FIRE_RATE,
        SHIELD
    }

    public PowerUp(float x, float y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.width = 30;
        this.height = 30;
        this.image = new ImageIcon(getImagePath()).getImage();

    }

    private String getImagePath() {
        switch (type) {
            case EXTRA_LIFE:
                return "powerup_extra_life.png";
            case INCREASE_FIRE_RATE:
                return "powerup_fire_rate.png";
            case SHIELD:
                return "powerup_shield.png";
            default:
                return "powerup_default.png";
        }
    }

    // Ruch ulepszenia w dół ekranu
    public void move() {
        y += dy;
    }

    // Rysowanie ulepszenia
    public void draw(Graphics2D g2d) {
        g2d.drawImage(image, (int)x, (int)y, width, height, null);
    }

    // Pobieranie hitboxa dla kolizji
    public Shape getHitbox() {
        return new Rectangle2D.Float(x, y, width, height);
    }
}

// Klasa do przechowywania konfiguracji poziomów
class LevelConfig {
    private List<List<Float>> planePositionsPerRow;

    public LevelConfig(List<List<Float>> planePositionsPerRow) {
        this.planePositionsPerRow = planePositionsPerRow;
    }

    public List<List<Float>> getPlanePositionsPerRow() {
        return planePositionsPerRow;
    }

    public int getRows() {
        return planePositionsPerRow.size();
    }

    public List<Float> getPlanePositionsInRow(int row) {
        if (row >= 0 && row < planePositionsPerRow.size()) {
            return planePositionsPerRow.get(row);
        }
        return new ArrayList<>();
    }
}

// Klasa bazowa dla wszystkich wrogów
abstract class EnemyPlane extends Rectangle2D.Float {
    boolean isVisible = true;
    Image image;  // Obraz samolotu
    EnemyBulletType bulletType;
    protected Plansza p;

    EnemyPlane(float x, float y, int w, int h, EnemyBulletType bulletType, Plansza p) {
        super(x, y, w, h);
        this.bulletType = bulletType;
        this.p = p;
        loadAndRotateImage(getImagePath());
    }

    protected abstract String getImagePath();
    

    protected void loadAndRotateImage(String path) {
        BufferedImage originalImage = loadImage(path);
        if (originalImage != null) {
            BufferedImage rotatedImage = rotateImage180(originalImage);
            this.image = rotatedImage;
        } else {
            // Jeśli nie udało się załadować obrazu - tworzymy zaślepkę
            this.image = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = ((BufferedImage) this.image).createGraphics();
            g2d.setColor(Color.BLUE);
            g2d.fillRect(0, 0, (int)width, (int)height);
            g2d.dispose();
        }
    }
    protected BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch(IOException e) {
            System.err.println("Nie udało się załadować obrazu: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private BufferedImage rotateImage180(BufferedImage src) {
        AffineTransform transform = new AffineTransform();
        // Obrót o 180 stopni wokół środka obrazu
        transform.rotate(Math.toRadians(180), src.getWidth() / 2.0, src.getHeight() / 2.0);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage rotated = op.filter(src, null);
        return rotated;
    }

    // Rysowanie samolotu
    void draw(Graphics2D g2d) {
        g2d.drawImage(image, (int) x, (int) y, (int) width, (int) height, null);
    }

    // Metoda do strzelania pociskami
    public void shoot() {
        float bulletStartX = this.x + this.width / 2f - 4;
        float bulletStartY = this.y + this.height;
        Bullet enemyBullet;
        switch (bulletType) {
            case TYPE1:
                enemyBullet = new EnemyBulletType1(p, bulletStartX, bulletStartY);
                break;
            case TYPE2:
                enemyBullet = new EnemyBulletType2(p, bulletStartX, bulletStartY);
                break;

            case TYPE3:
                enemyBullet = new EnemyBulletType3(p, bulletStartX, bulletStartY);
                break;
            case TYPE4:
                enemyBullet = new EnemyBulletType4(p, bulletStartX, bulletStartY);
                break;
            default:
                enemyBullet = new EnemyBulletType1(p, bulletStartX, bulletStartY);
                break;
        }
        p.addBullet(enemyBullet);
    }

}

// Klasa reprezentująca bossa
abstract class BossPlane extends EnemyPlane {
    int health;

    BossPlane(float x, float y, int w, int h, int health, EnemyBulletType bulletType, Plansza p) {
        super(x, y, w, h, bulletType, p);
        this.health = health;
    }


    protected abstract String getImagePath();

   
    void draw(Graphics2D g2d) {
        super.draw(g2d);
        // Rysowanie paska zdrowia bossa nad nim
        g2d.setColor(Color.RED);
        g2d.fillRect((int) x, (int) (y - 10), (int) width, 5);
        g2d.setColor(Color.GREEN);
        double healthRatio = Math.max(0, (double) health / getMaxHealth());
        g2d.fillRect((int) x, (int) (y - 10), (int) (width * healthRatio), 5);
    }

    protected abstract int getMaxHealth();

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            isVisible = false;
        }
    }
}

// Boss typu 1
class BossType1 extends BossPlane {
    public BossType1(float x, float y, Plansza p) {
        super(x, y, 250, 150, 200, EnemyBulletType.TYPE1, p);
    }


    protected String getImagePath() {
        return "boss1.png";
    }


    protected int getMaxHealth() {
        return 200;
    }
}

// Boss typu 2
class BossType2 extends BossPlane {
    public BossType2(float x, float y, Plansza p) {
        super(x, y, 300, 180, 300, EnemyBulletType.TYPE2, p);
    }


    protected String getImagePath() {
        return "boss.png";
    }


    protected int getMaxHealth() {
        return 300;
    }
}

// Wrogowie typu 1
class EnemyType1 extends EnemyPlane {
    public EnemyType1(float x, float y, Plansza p) {
        super(x, y, 40, 30, EnemyBulletType.TYPE1, p);
    }

    protected String getImagePath() {
        return "mob_small.png";
    }
}

// Wrogowie typu 2
class EnemyType2 extends EnemyPlane {
    public EnemyType2(float x, float y, Plansza p) {
        super(x, y, 50, 35, EnemyBulletType.TYPE2, p);
    }

    protected String getImagePath() {
        return "mob2.png";
    }
}

// Wrogowie typu 3
class EnemyType3 extends EnemyPlane {
    public EnemyType3(float x, float y, Plansza p) {
        super(x, y, 45, 32, EnemyBulletType.TYPE3, p);
    }

    protected String getImagePath() {
        return "mob3.png";
    }
}

// Wrogowie typu 4
class EnemyType4 extends EnemyPlane {
    public EnemyType4(float x, float y, Plansza p) {
        super(x, y, 55, 40, EnemyBulletType.TYPE4, p);
    }

    protected String getImagePath() {
        return "mob4.png";
    }
}

// Klasa reprezentująca statek gracza
class Belka {
    float x, y;
    int width;
    int height;
    Image imageRight;
    Image imageLeft;
    Image imageStand;
    Image currentImage;
    private boolean invulnerable = false; 
    private long invulEndTime = 0; 


    // Dokładny hitbox
    Shape hitbox;
    
    private boolean shieldActive = false;
    private long shieldEndTime = 0;

    // Metoda do aktywacji tarczy
    public void activateShield() {
        shieldActive = true;
        shieldEndTime = System.currentTimeMillis() + 5000; // Tarcza aktywna przez 5 sekund
    }

    // Metoda do dezaktywacji tarczy
    public void deactivateShield() {
        shieldActive = false;
    }

    // Metoda do sprawdzania statusu tarczy
    public boolean isShieldActive() {
        return shieldActive;
    }

    // Aktualizacja stanu tarczy (można wywoływać z metod ruchu lub timerów)
    public void updateShield() {
        if (shieldActive && System.currentTimeMillis() > shieldEndTime) {
            deactivateShield();
        }
    }

    Belka(float x, float y) {
        this.x = x;
        this.y = y;
        this.width = 70;
        this.height = 70;

        this.imageRight = new ImageIcon("planeUserRight.png").getImage();
        this.imageLeft = new ImageIcon("planeUserLeft.png").getImage();
        this.imageStand = new ImageIcon("planeUserStand.png").getImage();
        this.currentImage = imageStand;

        // Tworzymy początkowy Polygon (umowny kształt statku)
        Polygon p = new Polygon();
        p.addPoint(10, 0);   // Górny punkt
        p.addPoint(60, 35);  // Prawy róg
        p.addPoint(10, 70);  // Dolny punkt

        // Przesunięcie (x, y)
        AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
        hitbox = tx.createTransformedShape(p);
    }

    void setX(float newX) {
        this.x = newX;
        updateHitbox();
    }

    void setDirection(String direction) {
        switch (direction.toLowerCase()) {
            case "right":
                currentImage = imageRight;
                break;
            case "left":
                currentImage = imageLeft;
                break;
            default:
                currentImage = imageStand;
                break;
        }
    }

   // W klasie Belka, poniżej innych metod
    public void becomeInvulnerable(int durationMs) {
    invulnerable = true;
    invulEndTime = System.currentTimeMillis() + durationMs; 
    // Na przykład, jeśli 1000, to 1 sekunda nietykalności
}

// Metoda, która zwraca true, jeśli jesteśmy jeszcze w trybie nietykalności
    public boolean isInvulnerable() {
    // Jeśli czas minął, wyłączamy
    if (invulnerable && System.currentTimeMillis() > invulEndTime) {
        invulnerable = false;
    }
    return invulnerable;
}
   
    // Aktualizuje kształt hitboxa zgodnie z współrzędnymi (x, y)
    private void updateHitbox() {
        Polygon p = new Polygon();
        p.addPoint(10, 0);
        p.addPoint(60, 35);
        p.addPoint(10, 70);

        AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
        hitbox = tx.createTransformedShape(p);
    }

    void draw(Graphics2D g2d) {

        // Jeśli teraz jest nietykalność, zrobimy migotanie: 
    // jedna klatka rysujemy, drugą – pomijamy
    if (isInvulnerable()) {
        long now = System.currentTimeMillis();
        // Na przykład, jeśli (now / 100) % 2 == 0, pominiemy rysowanie
        if (((now / 100) % 2) == 0) {
            // Po prostu return, czyli nie rysujemy tej klatki
            return; 
        }
    }
        g2d.drawImage(currentImage, (int)x, (int)y, width, height, null);
         // Rysujemy tarczę, jeśli jest aktywna
         if (shieldActive) {
            g2d.setColor(new Color(0, 0, 255, 100)); // Półprzezroczysty niebieski
            g2d.fillOval((int)x - 10, (int)y - 10, width + 20, height + 20);
        }



    }

    public Shape getHitbox() {
        return hitbox;
    }
}

class EnemyBulletType1 extends Bullet {
    public EnemyBulletType1(Plansza p, float startX, float startY) {
        super(p, startX, startY, 0, 4, BulletOwner.ENEMY, loadImage("enemy_bullet2.png"));
        this.width = 15;
        this.height = 26;
    }

    protected void initializeHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }
 
    protected void updateHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void checkCollision() {
        // Jeśli gracz jest już nietykalny - wychodzimy
        if (p.b.isInvulnerable()) {
            return;
        }
        // Kolizja ze statkiem gracza
        Area bulletArea = new Area(hitbox);
        Area belkaArea = new Area(p.b.getHitbox());
        belkaArea.intersect(bulletArea);
        if (!belkaArea.isEmpty()) {
            p.playerHit();
            p.removeBullet(this);
        }
    }

    private static Image loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch(IOException e) {
            System.err.println("Nie udało się załadować: " + path);
            e.printStackTrace();
            // Tworzymy zaślepkę
            BufferedImage temp = new BufferedImage(8, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setColor(Color.MAGENTA);
            g2.fillRect(0, 0, 8, 16);
            g2.dispose();
            return temp;
        }
    }
}

// Pocisk wroga typu 2
class EnemyBulletType2 extends Bullet {
    public EnemyBulletType2(Plansza p, float startX, float startY) {
        super(p, startX, startY, 0, 6, BulletOwner.ENEMY, loadImage("enemy_bullet1.png"));
        this.width = 19;
        this.height = 30;
    }

    protected void initializeHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void updateHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void checkCollision() {
        if (p.b.isInvulnerable()) {
            return;
        }
        // Kolizja ze statkiem gracza
        Area bulletArea = new Area(hitbox);
        Area belkaArea = new Area(p.b.getHitbox());
        belkaArea.intersect(bulletArea);
        if (!belkaArea.isEmpty()) {
            p.playerHit();
            p.removeBullet(this);
        }
    }

    private static Image loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch(IOException e) {
            System.err.println("Nie udało się załadować: " + path);
            e.printStackTrace();
            // Tworzymy zaślepkę
            BufferedImage temp = new BufferedImage(12, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setColor(Color.ORANGE);
            g2.fillRect(0, 0, 12, 24);
            g2.dispose();
            return temp;
        }
    }
}

// Pocisk wroga typu 3
class EnemyBulletType3 extends Bullet {
    public EnemyBulletType3(Plansza p, float startX, float startY) {
        super(p, startX, startY, 0, 4, BulletOwner.ENEMY, loadImage("enemy_bullet3.png"));
        this.width = 20;
        this.height = 30;
    }

    protected void initializeHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void updateHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void checkCollision() {
        if (p.b.isInvulnerable()) {
            return;
        }
        // Kolizja ze statkiem gracza
        Area bulletArea = new Area(hitbox);
        Area belkaArea = new Area(p.b.getHitbox());
        belkaArea.intersect(bulletArea);
        if (!belkaArea.isEmpty()) {
            p.playerHit();
            p.removeBullet(this);
        }
    }

    private static Image loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch(IOException e) {
            System.err.println("Nie udało się załadować: " + path);
            e.printStackTrace();
            // Tworzymy zaślepkę
            BufferedImage temp = new BufferedImage(8, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setColor(Color.MAGENTA);
            g2.fillRect(0, 0, 8, 16);
            g2.dispose();
            return temp;
        }
    }
}

// Pocisk wroga typu 4
class EnemyBulletType4 extends Bullet {
    public EnemyBulletType4(Plansza p, float startX, float startY) {
        super(p, startX, startY, 0, 4, BulletOwner.ENEMY, loadImage("enemy_bullet4.png"));
        this.width = 15;
        this.height = 26;
    }

    protected void initializeHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void updateHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void checkCollision() {
        if (p.b.isInvulnerable()) {
            return;
        }
        // Kolizja ze statkiem gracza
        Area bulletArea = new Area(hitbox);
        Area belkaArea = new Area(p.b.getHitbox());
        belkaArea.intersect(bulletArea);
        if (!belkaArea.isEmpty()) {
            p.playerHit();
            p.removeBullet(this);
        }
    }

    private static Image loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch(IOException e) {
            System.err.println("Nie udało się załadować: " + path);
            e.printStackTrace();
            // Tworzymy zaślepkę
            BufferedImage temp = new BufferedImage(8, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setColor(Color.MAGENTA);
            g2.fillRect(0, 0, 8, 16);
            g2.dispose();
            return temp;
        }
    }
}

// Pocisk gracza
class PlayerBullet extends Bullet {
    public PlayerBullet(Plansza p, float startX, float startY) {
        super(p, startX, startY, 0, -5, BulletOwner.PLAYER, loadImage("bullet_simple.png"));
        this.width = 15;
        this.height = 25;
    }

    public PlayerBullet setDirection(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
        return this;
    }

    protected void initializeHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void updateHitbox() {
        hitbox = new Rectangle2D.Float(x, y, width, height);
    }

    protected void checkCollision() {
        // Sprawdzanie kolizji z wrogami
        for (EnemyPlane plane : p.enemyPlanes) {
            if (plane.isVisible && hitbox.intersects(plane.getBounds2D())) {

                // 1) Obliczamy pożądane współrzędne dla wybuchu:
                int explosionW = p.explosionGif.getWidth(null);
                int explosionH = p.explosionGif.getHeight(null);
                
                float explosionX = plane.x + plane.width  / 4f - explosionW / 4f;
                float explosionY = plane.y + plane.height / 4f - explosionH / 4f;

                // Jeśli to boss
                if (plane instanceof BossPlane) {
                    BossPlane boss = (BossPlane) plane;
                    boss.takeDamage(5); // Boss otrzymuje więcej obrażeń
                    if (!boss.isVisible) {
                        p.incrementScore(100); // Bonus za zniszczenie bossa
                        // Wypuszczenie ulepszenia z bossów
                        p.spawnRandomPowerUp(plane.x, plane.y);
                        p.addExplosion(explosionX, explosionY);
                    }
                } else {
                    // Zwykły wróg
                    plane.isVisible = false;
                    p.incrementScore();
                    // Wypuszczenie ulepszenia ze zwykłych wrogów
                    p.spawnRandomPowerUp(plane.x, plane.y);
                    p.addExplosion(explosionX, explosionY);
                }
                p.removeBullet(this);
                break;
            }
        }
    }

    private static Image loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch(IOException e) {
            System.err.println("Nie udało się załadować: " + path);
            e.printStackTrace();
            // Tworzymy zaślepkę
            BufferedImage temp = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setColor(Color.YELLOW);
            g2.fillRect(0, 0, 10, 20);
            g2.dispose();
            return temp;
        }
    }
}

// Główna klasa gry
class Plansza extends JPanel implements MouseMotionListener, MouseListener {
    // Dodano obraz serduszka
    private Image heartImage;
    private int heartWidth = 50; 
    private int heartHeight = 28;
    Image backgroundImage;

    // Liczba żyć
    private int lives = 3;

    Belka b;                          // Statek
    ArrayList<EnemyPlane> enemyPlanes;// Wrogie samoloty
    ArrayList<Bullet> bullets;        // Pociski
    ArrayList<PowerUp> powerUps;
    Image explosionGif; // GIF wybuchu
    private List<Explosion> explosions;

    int score = 0;
    long lastHitTime = 0;
    int comboCount = 0;
    boolean gameOver = false;
    int currentLevel = 1;            // Zaczynamy od 1-go poziomu
    final int maxLevels;
    int bricksInLevel;
    int bricksDestroyed = 0;

    

// W klasie Plansza
private boolean isPaused = false;
private int countdown = 3;        // Licznik (sekundy)
private boolean countdownActive = true; // Pokazuje, czy odliczanie jest aktywne

// Timer dla strzelania gracza (ciągłe, gdy lewy przycisk myszy jest wciśnięty)
private Timer shootingTimer;
private static final int SHOOT_DELAY = 200; 

// Timer dla aktualizacji pocisków
private Timer gameTimer; 
private static final int GAME_DELAY = 15; 

// Timer dla powrotu statku do pozycji "stand"
private Timer standTimer;
private static final int STAND_DELAY = 300; 
private int previousMouseX = -1;

// Poziomy
private List<LevelConfig> levelConfigs;

// Timery dla wrogów
private Timer enemyShootingTimer;
private static final int ENEMY_SHOOT_DELAY = 3000;
private Timer enemyMoveTimer;
private int enemyDirection = 1; 
private int enemySpeed = 1; 
private boolean levelInitialized = false;
private boolean powerUpDroppedThisLevel = false;

// Konstruktor
Plansza() {
    super();
    addMouseMotionListener(this);
    addMouseListener(this);
    setLayout(null);

    backgroundImage = loadImage("backgroung.jpg");
    heartImage = loadImage("heart.png");
    explosionGif = new ImageIcon("boom3.gif").getImage();

    b = new Belka(360, 700);
    enemyPlanes = new ArrayList<>();
    bullets = new ArrayList<>();
    powerUps = new ArrayList<>(); 
    explosions = new ArrayList<>();

    shootingTimer = new Timer(SHOOT_DELAY, e -> shootBullet());
    gameTimer = new Timer(GAME_DELAY, e -> updateBullets());
    gameTimer.start();

    standTimer = new Timer(STAND_DELAY, e -> b.setDirection("stand"));
    standTimer.setRepeats(false);

    initializeLevelConfigs();
    maxLevels = levelConfigs.size();

    enemyShootingTimer = new Timer(ENEMY_SHOOT_DELAY, e -> enemyShoot());
    enemyShootingTimer.start();
    enemyMoveTimer = new Timer(30, e -> moveEnemies());
    enemyMoveTimer.start();

    // gdzieś w konstruktorze Plansza:
    Timer startTimer = new Timer(1000, e -> {
        countdown--;
        if (countdown <= 0) {
            countdownActive = false;
            ((Timer)e.getSource()).stop(); // zatrzymujemy ten timer
        }
        repaint(); // przerysowujemy, aby zaktualizować wyświetlaną cyfrę
    });
    startTimer.start();

    // Inicjalizacja poziomu po wyświetleniu komponentów (aby znać rozmiar ekranu)
    addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
            if (!levelInitialized && getWidth() > 0) {
                initializeLevel(currentLevel);
                levelInitialized = true;
            }
        }

        public void componentShown(ComponentEvent e) {
            if (!levelInitialized && getWidth() > 0) {
                initializeLevel(currentLevel);
                levelInitialized = true;
            }
        }
    });

    // Słuchacz klawiatury
    setFocusable(true);
    requestFocusInWindow();
    addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    if (gameOver == false){
                        togglePause();
                    }
                    break;
            }
        }
        
        public void keyReleased(KeyEvent e) {
            b.setDirection("stand");
        }
    });
}

private Image loadImage(String path) {
    try {
        return ImageIO.read(new File(path));
    } catch(IOException e) {
        System.err.println("Nie udało się załadować obrazu: " + path);
        e.printStackTrace();
        return null;
    }
}

// Metoda do tworzenia i dodawania ulepszenia
public void spawnRandomPowerUp(float x, float y) {
    if (powerUpDroppedThisLevel) {
        return; // Już wypadło ulepszenie na tym poziomie
    }

    double dropChance = 0.2; // 20% szansa na wypadnięcie ulepszenia
    if (Math.random() <= dropChance) {
        PowerUp.PowerUpType type = getRandomPowerUpType();
        PowerUp powerUp = new PowerUp(x, y, type);
        powerUps.add(powerUp);
        powerUpDroppedThisLevel = true; // Zaznaczamy, że ulepszenie już wypadło
    }
}

// Metoda do losowego wyboru typu ulepszenia
private PowerUp.PowerUpType getRandomPowerUpType() {
    double rand = Math.random();
    if (rand < 0.33) {
        return PowerUp.PowerUpType.EXTRA_LIFE;
    } else if (rand < 0.66) {
        return PowerUp.PowerUpType.INCREASE_FIRE_RATE;
    } else {
        return PowerUp.PowerUpType.SHIELD;
    }
}

private void applyPowerUp(PowerUp.PowerUpType type) {
    switch (type) {
        case EXTRA_LIFE:
            lives++;
            break;
        case INCREASE_FIRE_RATE:
            FireRate();
            break;
        case SHIELD:
            activateShield();
            break;
        // Dodaj inne przypadki w razie potrzeby
    }
}

private void FireRate() {
    // Zatrzymujemy bieżący timer strzelania
    shootingTimer.stop(); 

    // Tworzymy nowy timer z wzmocnionym strzelaniem
    shootingTimer = new Timer(SHOOT_DELAY, e -> {
        // Standardowa centralna kula
        addBullet(new PlayerBullet(this, b.x + b.width / 2f - 5, b.y));
        // Lewa kula
        addBullet(new PlayerBullet(this, b.x + b.width / 2f - 15, b.y).setDirection(-1, -2));
        // Prawa kula
        addBullet(new PlayerBullet(this, b.x + b.width / 2f + 5, b.y).setDirection(1, -2));
    });

    shootingTimer.start(); // Uruchamiamy strzelanie

    // Timer do powrotu strzelania do standardowego po 4 sekundach
    Timer resetTimer = new Timer(4000, e -> {
        shootingTimer.stop(); // Zatrzymujemy wzmocnione strzelanie

        // Przywracamy standardowe strzelanie
        shootingTimer = new Timer(SHOOT_DELAY, ev -> {
            addBullet(new PlayerBullet(this, b.x + b.width / 2f - 5, b.y));
        });
        shootingTimer.start();
    });

    resetTimer.setRepeats(false); // Uruchamiamy tylko raz
    resetTimer.start();
}

private void activateShield() {
    b.activateShield();
    // Ustawiamy timer do wyłączenia tarczy po określonym czasie
    Timer shieldTimer = new Timer(5000, e -> b.deactivateShield());
    shieldTimer.setRepeats(false);
    shieldTimer.start();
}

private void togglePause() {
    isPaused = !isPaused;

    if (isPaused) {
        // Zatrzymujemy wszystkie timery
        shootingTimer.stop();
        gameTimer.stop();
        enemyShootingTimer.stop();
        enemyMoveTimer.stop();
    } else {
        // Uruchamiamy wszystkie timery
        gameTimer.start();
        enemyShootingTimer.start();
        enemyMoveTimer.start();
    }

    // Przerysowujemy komponent, aby wyświetlić komunikat o pauzie
    repaint();
}

private void initializeLevelConfigs() {
    levelConfigs = new ArrayList<>();

    // Poziom 1
    levelConfigs.add(new LevelConfig(Arrays.asList(
        Arrays.asList(0.1f, 0.3f, 0.5f, 0.7f, 0.9f),
        Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f),
        Arrays.asList(0.3f, 0.5f, 0.7f),
        Arrays.asList(0.4f, 0.6f)
    )));

    // Poziom 2
    levelConfigs.add(new LevelConfig(Arrays.asList(
        Arrays.asList(0.05f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.95f),
        Arrays.asList(0.1f, 0.3f, 0.5f, 0.7f, 0.9f),
        Arrays.asList(0.15f, 0.35f, 0.55f, 0.75f, 0.95f),
        Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f),
        Arrays.asList(0.25f, 0.45f, 0.65f, 0.85f)
    )));

    // Poziom 3
    levelConfigs.add(new LevelConfig(Arrays.asList(
        Arrays.asList(0.05f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.95f),
        Arrays.asList(0.1f, 0.3f, 0.5f, 0.7f, 0.9f),
        Arrays.asList(0.15f, 0.35f, 0.55f, 0.75f, 0.95f),
        Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f),
        Arrays.asList(0.25f, 0.45f, 0.65f, 0.85f),
        Arrays.asList(0.3f, 0.5f, 0.7f, 0.9f)
    )));

    // Poziom 4 (Boss typu 1)
    levelConfigs.add(new LevelConfig(Arrays.asList(
        Arrays.asList(0.5f), // Rząd dla bossa
        Arrays.asList(0.1f, 0.3f, 0.7f, 0.9f),
        Arrays.asList(0.15f, 0.35f, 0.65f, 0.85f),
        Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f)
    )));

    // Poziom 5
    levelConfigs.add(new LevelConfig(Arrays.asList(
        Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f),
        Arrays.asList(0.3f, 0.5f, 0.7f),
        Arrays.asList(0.4f, 0.6f)
    )));

    // Poziom 6 (Boss typu 2)
    levelConfigs.add(new LevelConfig(Arrays.asList(
        Arrays.asList(0.5f), // Rząd dla bossa
        Arrays.asList(0.1f, 0.3f, 0.7f, 0.9f),
        Arrays.asList(0.15f, 0.35f, 0.65f, 0.85f),
        Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f),
        Arrays.asList(0.25f, 0.45f, 0.65f, 0.85f)
    )));

    for (int i = 0; i < levelConfigs.size(); i++) {
        System.out.println("Poziom " + (i + 1) + ": " + levelConfigs.get(i).getPlanePositionsPerRow());
    }
}

private void initializeLevel(int level) {
    if (level < 1 || level > levelConfigs.size()) {
        System.err.println("Poziom " + level + " nie jest zdefiniowany.");
        return;
    }

    enemyPlanes.clear();
    bricksDestroyed = 0;

    // Resetujemy flagę, aby ulepszenia mogły ponownie wypadać
    powerUpDroppedThisLevel = false;

    LevelConfig config = levelConfigs.get(level - 1);
    List<List<Float>> planePositionsPerRow = config.getPlanePositionsPerRow();

    System.out.println("Inicjalizacja Poziomu " + level + " z konfiguracją: " + planePositionsPerRow);

    int planeWidth = 40;
    int planeHeight = 30;
    int startY = 40;
    int gapY = 5;

    for (int r = 0; r < config.getRows(); r++) {
        List<Float> planePositions = config.getPlanePositionsInRow(r);
        if (planePositions.isEmpty()) continue;

        for (float relativeX : planePositions) {
            float xPos = relativeX * getWidth() - planeWidth / 2.0f;
            if (xPos < 0) xPos = 0;
            if (xPos + planeWidth > getWidth()) {
                xPos = getWidth() - planeWidth;
            }
            float yPos = startY + r * (planeHeight + gapY);

            // Określamy typ wroga w zależności od poziomu lub rzędu
            EnemyPlane enemy;
            // Logika z bossami
            if (level == 4 && r == 0 && planePositions.size() == 1) {
                // Dla poziomu 4: boss typu 1 (lub 2, jeśli chcesz naprzemiennie)
                enemy = new BossType1(xPos, yPos, this);
                enemyPlanes.add(enemy);
            } else if (level == 6 && r == 0 && planePositions.size() == 1) {
                // Dla poziomu 6: boss typu 2
                enemy = new BossType2(xPos, yPos, this);
                enemyPlanes.add(enemy);
            } else {
                // W zależności od poziomu zmieniamy typy wrogów
                if (level == 1) {
                    if (r % 2 == 0) {
                        enemy = new EnemyType1(xPos, yPos, this);
                    } else {
                        enemy = new EnemyType2(xPos, yPos, this);
                    }
                } else if (level == 2) {
                    if (r % 2 == 0) {
                        enemy = new EnemyType2(xPos, yPos, this);
                    } else {
                        enemy = new EnemyType3(xPos, yPos, this);
                    }
                } else if (level == 3) {
                    enemy = new EnemyType1(xPos, yPos, this);
                } else if (level == 5) {
                    enemy = new EnemyType4(xPos, yPos, this);
                } else {
                    // Domyślnie typ 1 (w przykładzie)
                    enemy = new EnemyType1(xPos, yPos, this);
                }
                enemyPlanes.add(enemy);
            }
        }
    }

    bricksInLevel = enemyPlanes.size();
    comboCount = 0;
    lastHitTime = 0;
    gameOver = false;
}

private void shootBullet() {
    Bullet bullet = new PlayerBullet(this, b.x + b.width / 2f - 5, b.y);
    addBullet(bullet);
}

private void updateBullets() {
    ArrayList<Bullet> bulletsCopy = new ArrayList<>(bullets);
    for (Bullet bullet : bulletsCopy) {
        bullet.move();
    }

    // Aktualizacja ulepszeń
    ArrayList<PowerUp> powerUpsCopy = new ArrayList<>(powerUps);
    for (PowerUp powerUp : powerUpsCopy) {
        powerUp.move();
        // Sprawdzanie, czy ulepszenie wyleciało poza ekran
        if (powerUp.y > getHeight()) {
            powerUps.remove(powerUp);
            continue;
        }

        // Sprawdzanie kolizji ze statkiem gracza
        if (powerUp.getHitbox().intersects(b.getHitbox().getBounds2D())) {
            applyPowerUp(powerUp.type);
            powerUps.remove(powerUp);
        }
    }

    // Aktualizujemy wybuchy
    ArrayList<Explosion> explosionsCopy = new ArrayList<>(explosions);
    for (Explosion explosion : explosionsCopy) {
        explosion.update();
        if (explosion.isFinished()) {
            explosions.remove(explosion);
        }
    }

    // Aktualizacja stanu tarczy
    b.updateShield();

    repaint();
}

void incrementScore() {
    long now = System.currentTimeMillis();
    if (now - lastHitTime <= 1000) {
        comboCount++;
    } else {
        comboCount = 1;
    }
    lastHitTime = now;
    score += comboCount;
    bricksDestroyed++;

    if (bricksDestroyed >= bricksInLevel) {
        if (currentLevel < maxLevels) {
            currentLevel++;
            System.out.println("Przejście do poziomu " + currentLevel);
            initializeLevel(currentLevel);
        } else {
            gameOver = true;
            shootingTimer.stop();
            gameTimer.stop();
            enemyShootingTimer.stop();
            enemyMoveTimer.stop();
            JOptionPane.showMessageDialog(this, "WYGRAŁEŚ! Zdobyłeś " + score + " punktów");
            Timer timer = new Timer(5000, e -> {
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                parentFrame.dispose();
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
}

void incrementScore(int bonus) {
    long now = System.currentTimeMillis();
    if (now - lastHitTime <= 1000) {
        comboCount++;
    } else {
        comboCount = 1;
    }
    lastHitTime = now;
    score += bonus * comboCount;
    bricksDestroyed++;

    if (bricksDestroyed >= bricksInLevel) {
        if (currentLevel < maxLevels) {
            currentLevel++;
            System.out.println("Przejście do poziomu " + currentLevel);
            initializeLevel(currentLevel);
        } else {
            gameOver = true;
            shootingTimer.stop();
            gameTimer.stop();
            enemyShootingTimer.stop();
            enemyMoveTimer.stop();
            JOptionPane.showMessageDialog(this, "WYGRAŁEŚ! Zdobyłeś " + score + " punktów");
            Timer timer = new Timer(5000, e -> {
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                parentFrame.dispose();
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
}

public void playerHit() {
    if (b.isShieldActive()) {
        // Tarcza pochłania uderzenie, nie odejmujemy życia
        b.deactivateShield(); // Tarcza dezaktywuje się po pochłonięciu uderzenia
        // Można dodać dźwięk lub efekt wizualny
    } else {
        if (!gameOver) {
            lives--;
            if (lives <= 0) {
                gameOver = true;
                shootingTimer.stop();
                gameTimer.stop();
                enemyShootingTimer.stop();
                enemyMoveTimer.stop();

                JOptionPane.showMessageDialog(this, 
                        "Przegrałeś! Twój wynik: " + score);

                Timer timer = new Timer(5000, e -> {
                    JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                    parentFrame.dispose();
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                b.becomeInvulnerable(2500);
            }
        }
    }
}

public void addBullet(Bullet b) {
    bullets.add(b);
}

public void removeBullet(Bullet b) {
    bullets.remove(b);
}

public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (backgroundImage != null) {
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }

    Graphics2D g2d = (Graphics2D) g.create();

    // Rysujemy statek
    b.draw(g2d);

    // Rysujemy wrogów
    for (EnemyPlane plane : enemyPlanes) {
        if (plane.isVisible) {
            plane.draw(g2d);
        }
    }

    // Rysujemy wybuchy
    for (Explosion explosion : explosions) {
        explosion.draw(g2d);
    }

    // Rysujemy pociski
    for (Bullet bullet : bullets) {
        bullet.draw(g2d);
    }

    // Rysujemy ulepszenia
    for (PowerUp powerUp : powerUps) {
        powerUp.draw(g2d);
    }

    // Rysujemy punkty, poziom
    g2d.setColor(Color.BLACK);
    g2d.setFont(new Font("Arial", Font.BOLD, 16));
    g2d.drawString("Punkty: " + score, 10, 20);
    g2d.drawString("Poziom: " + currentLevel, 10, 40);

    // Rysujemy życia (serduszka)
    int heartX = getWidth() - heartWidth - 50; // Pozycja serduszka
    int heartY = 10; // Odległość od górnej krawędzi

    // Rysujemy serduszko
    g2d.drawImage(heartImage, heartX, heartY, heartWidth, heartHeight, null);

    // Rysujemy liczbę obok serduszka
    g2d.setColor(Color.BLACK);
    g2d.setFont(new Font("Arial", Font.BOLD, 16));
    String livesText = "x " + lives;
    int textX = heartX - 20; // Odstęp od serduszka
    int textY = heartY + heartHeight / 2 + 5; // Wyrównanie w pionie
    g2d.drawString(livesText, textX, textY);

    // Jeśli gra się zakończyła
    if (gameOver) {
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.RED);
        String msg;
        if (currentLevel > maxLevels) {
            msg = "WYGRAŁEŚ! Zdobyłeś " + score + " punktów";
        } else if (lives <= 0) {
            msg = "PRZEGRAŁEŚ! Twój wynik: " + score;
        } else {
            msg = "Gra zakończona!";
        }
        int msgWidth = g2d.getFontMetrics().stringWidth(msg);
        int x = (getWidth() - msgWidth) / 2;
        int y = getHeight() / 2;
        g2d.drawString(msg, x, y);
    }

    if (isPaused && countdownActive == false) {
        g2d.setColor(new Color(0, 0, 0, 100)); // Czarny z przezroczystością 100/255
        g2d.fillRect(0, 0, getWidth(), getHeight());

        String pauseMsg = "PAUZA";
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.setColor(new Color(0, 0, 0, 150)); // Półprzezroczysty czarny
        int msgWidth = g2d.getFontMetrics().stringWidth(pauseMsg);
        int x = (getWidth() - msgWidth) / 2;
        int y = getHeight() / 2;
        g2d.drawString(pauseMsg, x, y);
    }

    if (countdownActive && countdown > 0) {
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(new Color(0, 0, 0, 150));

        String text = String.valueOf(countdown);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        // Współrzędne, aby wyświetlić na środku ekranu
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - textHeight) / 2;

        g2d.drawString(text, x, y);
    }

    g2d.dispose();
}

public void mouseMoved(MouseEvent e) {
    if (isPaused || gameOver) return;

    int mouseX = e.getX();
    if (previousMouseX != -1) {
        if (mouseX > previousMouseX) {
            b.setDirection("right");
        } else if (mouseX < previousMouseX) {
            b.setDirection("left");
        }
    }
    previousMouseX = mouseX;

    float newX = mouseX - (b.width / 2f);
    if (newX < 0) newX = 0;
    if (newX + b.width > getWidth()) {
        newX = getWidth() - b.width;
    }
    b.setX(newX);
    repaint();
    standTimer.restart();
}

public void mouseDragged(MouseEvent e) {
    mouseMoved(e);
}

public void mousePressed(MouseEvent e) {
    // Jeśli trwa odliczanie - nie strzelamy
    if (countdownActive) return;
    shootingTimer.start();
}

public void addExplosion(float x, float y) {
    // Tworzymy obiekt wybuchu na podstawie GIF-a
    Explosion explosion = new Explosion(x, y, explosionGif);
    explosions.add(explosion);
}

public void mouseReleased(MouseEvent e) {
    shootingTimer.stop();
}

public void mouseClicked(MouseEvent e) {}
public void mouseEntered(MouseEvent e) {}
public void mouseExited(MouseEvent e) {}

private void enemyShoot() {
    if (countdownActive) return;
    for (EnemyPlane plane : enemyPlanes) {
        if (plane.isVisible) {
            plane.shoot();
        }
    }
}

private void moveEnemies() {
    boolean changeDirection = false;
    for (EnemyPlane plane : enemyPlanes) {
        if (plane.isVisible) {
            plane.x += enemySpeed * enemyDirection;
            // Jeśli dotknęli lewej/prawej krawędzi
            if (plane.x < 0 || plane.x + plane.width > getWidth()) {
                changeDirection = true;
            }
        }
    }
    if (changeDirection) {
        enemyDirection *= -1;
        for (EnemyPlane plane : enemyPlanes) {
            if (plane.isVisible) {
                plane.y += plane.height / 2;
                // Jeśli wrogowie zeszli do gracza
                if (plane.y + plane.height >= b.y) {
                    playerHit();
                    return;
                }
            }
        }
    }
    repaint();
}
}

// Główna klasa z metodą main()
public class Project {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Plansza p = new Plansza();
            JFrame jf = new JFrame();
            jf.add(p);
            jf.setTitle("Samolot");
            jf.setSize(800, 800);
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.setLocationRelativeTo(null);
            jf.setVisible(true);
        });
    }
}
