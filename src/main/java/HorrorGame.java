
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HorrorGame extends JPanel implements ActionListener, KeyListener {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Horror Rooms Prototype");
            HorrorGame game = new HorrorGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.start();
        });
    }

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 60;
    private static final int TILE = 32;

    private final javax.swing.Timer timer;
    private final InputState input = new InputState();
    private final AssetPack assets = new AssetPack();
    private final GameWorld world = new GameWorld();

    public HorrorGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new javax.swing.Timer(1000 / FPS, this);
    }

    private void start() {
        requestFocusInWindow();
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        world.update(input);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        world.render((Graphics2D) g);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        input.set(e.getKeyCode(), true);
        if (e.getKeyCode() == KeyEvent.VK_R && (world.gameOver || world.win)) {
            world.reset();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        input.set(e.getKeyCode(), false);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    static class InputState {
        boolean up;
        boolean down;
        boolean left;
        boolean right;
        boolean attack;
        boolean interact;

        void set(int keyCode, boolean pressed) {
            switch (keyCode) {
                case KeyEvent.VK_W, KeyEvent.VK_UP -> up = pressed;
                case KeyEvent.VK_S, KeyEvent.VK_DOWN -> down = pressed;
                case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = pressed;
                case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = pressed;
                case KeyEvent.VK_SPACE, KeyEvent.VK_F -> attack = pressed;
                case KeyEvent.VK_E -> interact = pressed;
            }
        }
    }

    class AssetPack {
        final BufferedImage dungeon = load("/assets/dungeon.png");
        final BufferedImage playerSheet = load("/assets/player.png");
        final BufferedImage gorkSheet = load("/assets/gorksprite.png");
        final BufferedImage skellySheet = load("/assets/skellysprite.png");

        final BufferedImage floorTile;
        final BufferedImage wallTile;
        final BufferedImage doorTileClosed;
        final BufferedImage doorTileOpen;
        final BufferedImage[] playerFrames;
        final BufferedImage[] gorkFrames;
        final BufferedImage[] skellyFrames;

        AssetPack() {
            floorTile = dungeon.getSubimage(224, 64, 32, 32);
            wallTile = dungeon.getSubimage(416, 96, 32, 32);
            doorTileClosed = dungeon.getSubimage(384, 384, 32, 64);
            doorTileOpen = dungeon.getSubimage(448, 384, 32, 64);

            playerFrames = splitPlayerFrames(playerSheet);
            gorkFrames = splitUniform(gorkSheet, 4, 4);
            skellyFrames = splitUniform(skellySheet, 4, 4);
        }

        private BufferedImage load(String resource) {
            try (InputStream in = HorrorGame.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Resource not found: " + resource);
                }
                return ImageIO.read(in);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load resource " + resource, ex);
            }
        }

        private BufferedImage[] splitUniform(BufferedImage sheet, int cols, int rows) {
            int fw = sheet.getWidth() / cols;
            int fh = sheet.getHeight() / rows;
            BufferedImage[] frames = new BufferedImage[cols * rows];
            int idx = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    frames[idx++] = sheet.getSubimage(c * fw, r * fh, fw, fh);
                }
            }
            return frames;
        }

        private BufferedImage[] splitPlayerFrames(BufferedImage sheet) {
            int cols = 8;
            int rows = 5;
            int fw = sheet.getWidth() / cols;
            int fh = sheet.getHeight() / rows;
            BufferedImage[] frames = new BufferedImage[cols * rows];
            int idx = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    frames[idx++] = sheet.getSubimage(c * fw, r * fh, fw, fh);
                }
            }
            return frames;
        }
    }

    class GameWorld {
        final List<Room> rooms = new ArrayList<>();
        final List<Door> doors = new ArrayList<>();
        final List<Monster> monsters = new ArrayList<>();
        final List<String> notifications = new ArrayList<>();

        Player player;
        boolean gameOver;
        boolean win;

        boolean interactLatch;
        boolean attackLatch;

        GameWorld() {
            reset();
        }

        void reset() {
            rooms.clear();
            doors.clear();
            monsters.clear();
            notifications.clear();
            gameOver = false;
            win = false;
            interactLatch = false;
            attackLatch = false;

            Room roomA = new Room("Storage", 70, 120, 270, 220);
            Room hallAB = new Room("Hall A", 340, 180, 120, 100);
            Room roomB = new Room("Ward", 460, 90, 320, 280);
            Room hallBC = new Room("Hall B", 780, 180, 120, 100);
            Room roomC = new Room("Chapel", 900, 110, 260, 240);
            Room safeRoom = new Room("Exit Chamber", 500, 430, 240, 180);

            rooms.add(roomA);
            rooms.add(hallAB);
            rooms.add(roomB);
            rooms.add(hallBC);
            rooms.add(roomC);
            rooms.add(safeRoom);

            doors.add(new Door(332, 204, 16, 52, false, "Storage Door"));
            doors.add(new Door(452, 204, 16, 52, false, "Ward Door"));
            doors.add(new Door(772, 204, 16, 52, false, "East Door"));
            doors.add(new Door(892, 204, 16, 52, false, "Chapel Door"));
            doors.add(new Door(610, 420, 52, 16, true, "Exit Door"));

            player = new Player(120, 180);

            monsters.add(new GorkMonster(585, 220));
            monsters.add(new SkeletonMonster(1010, 220));

            notifications.add("E - открыть дверь");
            notifications.add("SPACE/F - удар кулаком");
            notifications.add("Убей двух монстров и выйди через нижнюю дверь");
        }

        void update(InputState input) {
            if (gameOver || win) {
                return;
            }

            player.update(input, this);

            boolean interactPressed = input.interact && !interactLatch;
            interactLatch = input.interact;
            if (interactPressed) {
                for (Door door : doors) {
                    if (player.distanceTo(door.centerX(), door.centerY()) < 58) {
                        if (door.locked && monstersAlive() > 0) {
                            notifications.clear();
                            notifications.add("Дверь заперта. Сначала убей монстров.");
                        } else {
                            door.open = !door.open;
                            notifications.clear();
                            notifications.add(door.name + (door.open ? " открыта" : " закрыта"));
                        }
                        break;
                    }
                }
            }

            boolean attackPressed = input.attack && !attackLatch;
            attackLatch = input.attack;
            if (attackPressed) {
                player.punch(this);
            }

            for (Monster monster : monsters) {
                monster.update(this);
            }

            monsters.removeIf(m -> !m.alive);

            if (monstersAlive() == 0) {
                for (Door door : doors) {
                    if ("Exit Door".equals(door.name)) {
                        door.locked = false;
                    }
                }
            }

            Rectangle exitZone = new Rectangle(560, 500, 120, 80);
            if (monstersAlive() == 0 && exitZone.intersects(player.hitbox())) {
                win = true;
            }

            notifications.removeIf(s -> s == null || s.isBlank());
            while (notifications.size() > 3) {
                notifications.remove(0);
            }
        }

        int monstersAlive() {
            int count = 0;
            for (Monster m : monsters) {
                if (m.alive) count++;
            }
            return count;
        }

        boolean isWalkable(Rectangle rect) {
            Point[] points = new Point[]{
                    new Point(rect.x + 2, rect.y + 2),
                    new Point(rect.x + rect.width - 2, rect.y + 2),
                    new Point(rect.x + 2, rect.y + rect.height - 2),
                    new Point(rect.x + rect.width - 2, rect.y + rect.height - 2)
            };

            for (Point p : points) {
                boolean inRoom = false;
                for (Room room : rooms) {
                    if (room.contains(p.x, p.y)) {
                        inRoom = true;
                        break;
                    }
                }
                if (!inRoom) {
                    return false;
                }
            }

            for (Door door : doors) {
                if (!door.open && rect.intersects(door.bounds)) {
                    return false;
                }
            }
            return true;
        }

        void render(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g.setColor(new Color(8, 8, 12));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (Room room : rooms) {
                drawRoom(g, room);
            }

            for (Door door : doors) {
                drawDoor(g, door);
            }

            for (Monster monster : monsters) {
                monster.render(g);
            }

            player.render(g);

            drawHUD(g);
            drawDarkness(g);

            if (gameOver) {
                drawOverlay(g, "ТЕБЯ ПОЙМАЛИ", "Нажми R для рестарта");
            } else if (win) {
                drawOverlay(g, "ТЫ ВЫЖИЛ", "Нажми R для новой игры");
            }
        }

        void drawRoom(Graphics2D g, Room room) {
            for (int y = room.y; y < room.y + room.height; y += TILE) {
                for (int x = room.x; x < room.x + room.width; x += TILE) {
                    g.drawImage(assets.floorTile, x, y, TILE, TILE, null);
                }
            }

            for (int x = room.x - TILE; x <= room.x + room.width; x += TILE) {
                if (!isDoorGap(x, room.y - 8, true)) {
                    g.drawImage(assets.wallTile, x, room.y - TILE, TILE, TILE, null);
                }
                if (!isDoorGap(x, room.y + room.height - 8, true)) {
                    g.drawImage(assets.wallTile, x, room.y + room.height, TILE, TILE, null);
                }
            }

            for (int y = room.y; y < room.y + room.height; y += TILE) {
                if (!isDoorGap(room.x - 8, y, false)) {
                    g.drawImage(assets.wallTile, room.x - TILE, y, TILE, TILE, null);
                }
                if (!isDoorGap(room.x + room.width - 8, y, false)) {
                    g.drawImage(assets.wallTile, room.x + room.width, y, TILE, TILE, null);
                }
            }

            g.setColor(new Color(255, 255, 255, 40));
            g.drawString(room.name, room.x + 8, room.y + 18);
        }

        boolean isDoorGap(int x, int y, boolean horizontalCheck) {
            Rectangle probe = horizontalCheck ? new Rectangle(x, y, TILE, 16) : new Rectangle(x, y, 16, TILE);
            for (Door door : doors) {
                if (probe.intersects(door.bounds)) {
                    return true;
                }
            }
            return false;
        }

        void drawDoor(Graphics2D g, Door door) {
            BufferedImage img = door.open ? assets.doorTileOpen : assets.doorTileClosed;
            if (door.bounds.width > door.bounds.height) {
                g.drawImage(img, door.bounds.x, door.bounds.y - 20, door.bounds.width, 56, null);
            } else {
                g.drawImage(img, door.bounds.x - 18, door.bounds.y, 52, door.bounds.height, null);
            }
            if (door.locked && !door.open) {
                g.setColor(new Color(255, 40, 40, 200));
                g.drawString("LOCK", door.bounds.x - 4, door.bounds.y - 8);
            }
        }

        void drawHUD(Graphics2D g) {
            g.setFont(new Font("Dialog", Font.BOLD, 18));
            g.setColor(new Color(255, 255, 255, 230));
            g.drawString("HP: " + player.hp, 22, 32);
            g.drawString("Монстры: " + monstersAlive(), 22, 58);
            g.drawString("Управление: WASD / E / SPACE", 22, 84);

            int y = HEIGHT - 70;
            for (String note : notifications) {
                g.drawString(note, 22, y);
                y += 20;
            }
        }

        void drawDarkness(Graphics2D g) {
            BufferedImage fog = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D fg = fog.createGraphics();
            fg.setColor(new Color(0, 0, 0, 210));
            fg.fillRect(0, 0, WIDTH, HEIGHT);

            fg.setComposite(AlphaComposite.Clear);
            int radius = 210;
            fg.fillOval((int) player.x - radius + player.w / 2, (int) player.y - radius + player.h / 2, radius * 2, radius * 2);
            fg.fillPolygon(player.flashlightCone());

            fg.dispose();
            g.drawImage(fog, 0, 0, null);
        }

        void drawOverlay(Graphics2D g, String title, String subtitle) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Dialog", Font.BOLD, 48));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 2 - 10);

            g.setFont(new Font("Dialog", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            g.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, HEIGHT / 2 + 35);
        }
    }

    static class Room extends Rectangle {
        final String name;

        Room(String name, int x, int y, int w, int h) {
            super(x, y, w, h);
            this.name = name;
        }
    }

    static class Door {
        final Rectangle bounds;
        boolean open;
        boolean locked;
        final String name;

        Door(int x, int y, int w, int h, boolean locked, String name) {
            this.bounds = new Rectangle(x, y, w, h);
            this.locked = locked;
            this.name = name;
        }

        int centerX() {
            return bounds.x + bounds.width / 2;
        }

        int centerY() {
            return bounds.y + bounds.height / 2;
        }
    }

    abstract class Entity {
        double x;
        double y;
        int w;
        int h;
        boolean alive = true;

        Rectangle hitbox() {
            return new Rectangle((int) x, (int) y, w, h);
        }

        double centerX() {
            return x + w / 2.0;
        }

        double centerY() {
            return y + h / 2.0;
        }

        double distanceTo(double tx, double ty) {
            return Point2D.distance(centerX(), centerY(), tx, ty);
        }
    }

    class Player extends Entity {
        int hp = 6;
        double speed = 2.6;
        int facingX = 1;
        int facingY = 0;
        int animationFrame = 0;
        int animationTick = 0;
        long lastPunchTime = 0;
        long hurtCooldownUntil = 0;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.w = 28;
            this.h = 28;
        }

        void update(InputState input, GameWorld world) {
            double mx = 0;
            double my = 0;
            if (input.left) mx -= 1;
            if (input.right) mx += 1;
            if (input.up) my -= 1;
            if (input.down) my += 1;

            double len = Math.sqrt(mx * mx + my * my);
            if (len > 0) {
                mx /= len;
                my /= len;
                facingX = (int) Math.round(mx);
                facingY = (int) Math.round(my);
                animationTick++;
                if (animationTick % 8 == 0) {
                    animationFrame = (animationFrame + 1) % 4;
                }
            } else {
                animationFrame = 0;
            }

            attemptMove(mx * speed, 0, world);
            attemptMove(0, my * speed, world);
        }

        void attemptMove(double dx, double dy, GameWorld world) {
            Rectangle next = new Rectangle((int) (x + dx), (int) (y + dy), w, h);
            if (world.isWalkable(next)) {
                x += dx;
                y += dy;
            }
        }

        void punch(GameWorld world) {
            if (System.currentTimeMillis() - lastPunchTime < 260) {
                return;
            }
            lastPunchTime = System.currentTimeMillis();

            Rectangle strike = punchHitbox();
            boolean hit = false;
            for (Monster monster : world.monsters) {
                if (monster.alive && strike.intersects(monster.hitbox())) {
                    monster.hp -= 1;
                    monster.stun = 8;
                    hit = true;
                    if (monster.hp <= 0) {
                        monster.alive = false;
                        world.notifications.clear();
                        world.notifications.add(monster.name + " уничтожен");
                    }
                }
            }
            if (!hit) {
                world.notifications.clear();
                world.notifications.add("Ты бьешь в темноту...");
            }
        }

        Rectangle punchHitbox() {
            int reach = 34;
            int fx = facingX;
            int fy = facingY;
            if (fx == 0 && fy == 0) fx = 1;
            int px = (int) x + w / 2 + fx * reach - 16;
            int py = (int) y + h / 2 + fy * reach - 16;
            return new Rectangle(px, py, 32, 32);
        }

        Polygon flashlightCone() {
            int cx = (int) centerX();
            int cy = (int) centerY();
            int len = 250;
            int spread = 85;

            int fx = facingX;
            int fy = facingY;
            if (fx == 0 && fy == 0) fx = 1;

            int px = -fy;
            int py = fx;

            int x1 = cx + fx * 35 + px * spread;
            int y1 = cy + fy * 35 + py * spread;
            int x2 = cx + fx * (len + 30) + px * spread;
            int y2 = cy + fy * (len + 30) + py * spread;
            int x3 = cx + fx * (len + 30) - px * spread;
            int y3 = cy + fy * (len + 30) - py * spread;
            int x4 = cx + fx * 35 - px * spread;
            int y4 = cy + fy * 35 - py * spread;

            return new Polygon(new int[]{cx, x1, x2, x3, x4}, new int[]{cy, y1, y2, y3, y4}, 5);
        }

        void hurt(int damage, String source, GameWorld world) {
            long now = System.currentTimeMillis();
            if (now < hurtCooldownUntil) return;
            hurtCooldownUntil = now + 700;
            hp -= damage;
            world.notifications.clear();
            world.notifications.add("Тебя ранит " + source + " (-" + damage + " HP)");
            if (hp <= 0) {
                world.gameOver = true;
            }
        }

        void render(Graphics2D g) {
            BufferedImage frame = assets.playerFrames[animationFrame];
            int drawX = (int) x - 30;
            int drawY = (int) y - 30;
            g.drawImage(frame, drawX, drawY, 86, 86, null);

            Rectangle punch = punchHitbox();
            if (System.currentTimeMillis() - lastPunchTime < 80) {
                g.setColor(new Color(255, 230, 180, 90));
                g.fillRect(punch.x, punch.y, punch.width, punch.height);
            }
        }
    }

    abstract class Monster extends Entity {
        int hp;
        int damage;
        int stun;
        int frameIndex;
        int tick;
        double speed;
        String name;

        void update(GameWorld world) {
            if (!alive) return;
            tick++;
            if (tick % 12 == 0) frameIndex = (frameIndex + 1) % 4;

            if (stun > 0) {
                stun--;
                return;
            }

            double dx = world.player.centerX() - centerX();
            double dy = world.player.centerY() - centerY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0.0001) {
                dx /= dist;
                dy /= dist;
            }

            if (dist < 320) {
                tryMove(dx * speed, 0, world);
                tryMove(0, dy * speed, world);
            }

            if (hitbox().intersects(world.player.hitbox())) {
                world.player.hurt(damage, name, world);
            }
        }

        void tryMove(double dx, double dy, GameWorld world) {
            Rectangle next = new Rectangle((int) (x + dx), (int) (y + dy), w, h);
            if (world.isWalkable(next)) {
                x += dx;
                y += dy;
            }
        }

        abstract BufferedImage currentFrame();

        void render(Graphics2D g) {
            BufferedImage frame = currentFrame();
            g.drawImage(frame, (int) x - 10, (int) y - 10, 48, 48, null);

            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect((int) x, (int) y - 10, 34, 6);
            g.setColor(new Color(200, 30, 30));
            g.fillRect((int) x + 1, (int) y - 9, Math.max(0, (hp * 32) / maxHp()), 4);
        }

        abstract int maxHp();
    }

    class GorkMonster extends Monster {
        GorkMonster(int x, int y) {
            this.x = x;
            this.y = y;
            this.w = 28;
            this.h = 28;
            this.hp = 3;
            this.damage = 1;
            this.speed = 1.2;
            this.name = "Горк";
        }

        @Override
        BufferedImage currentFrame() {
            return assets.gorkFrames[frameIndex];
        }

        @Override
        int maxHp() {
            return 3;
        }
    }

    class SkeletonMonster extends Monster {
        SkeletonMonster(int x, int y) {
            this.x = x;
            this.y = y;
            this.w = 28;
            this.h = 28;
            this.hp = 4;
            this.damage = 2;
            this.speed = 1.6;
            this.name = "Скелет";
        }

        @Override
        BufferedImage currentFrame() {
            return assets.skellyFrames[frameIndex];
        }

        @Override
        int maxHp() {
            return 4;
        }
    }
}
