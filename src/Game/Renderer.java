/**
 * rscplus
 *
 * <p>This file is part of rscplus.
 *
 * <p>rscplus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>rscplus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with rscplus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * <p>Authors: see <https://github.com/RSCPlus/rscplus>
 */
package Game;

import Client.Launcher;
import Client.Logger;
import Client.NotificationsHandler;
import Client.NotificationsHandler.NotifType;
import Client.Settings;
import Client.Util;
import Client.WorldMapWindow;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;

/** Handles rendering overlays and client adjustments based on window size */
public class Renderer {

    public static Object instance = null;
    public static Graphics graphicsInstance = null;
    
    public static int GAME_RENDER_OFFSET = 11;

    public static int width;
    public static int height;
    public static int height_client;
    public static int[] pixels;
    public static int sprite_media;

    public static int fps;
    public static float alpha_time;
    public static float delta_time;
    public static long time;

    public static ImageConsumer image_consumer = null;

    public static Color color_dynamic;
    public static Color color_text = new Color(240, 240, 240);
    public static Color color_shadow = new Color(15, 15, 15);
    public static Color color_gray = new Color(60, 60, 60);
    public static Color color_hp = new Color(0, 210, 0);
    public static Color color_fatigue = new Color(210, 210, 0);
    public static Color color_prayer = new Color(160, 160, 210);
    public static Color color_low = new Color(255, 0, 0);
    public static Color color_poison = new Color(155, 205, 50);
    public static Color color_item = new Color(245, 245, 245);
    public static Color color_item_highlighted = new Color(245, 196, 70);
    public static Color color_replay = new Color(100, 185, 178);
    public static Color color_white = new Color(255, 255, 255);
    public static Color color_yellow = new Color(255, 255, 0);

    public static Image image_border;
    public static Image image_bar_frame;
    public static Image image_bar_frame_short;
    public static Image image_cursor;
    public static Image image_highlighted_item;
    public static Image image_wiki_hbar_inactive;
    public static Image image_wiki_hbar_active;
    private static BufferedImage game_image;
    
    private static Dimension new_size = new Dimension(0, 0);

    public static Font font_main;
    public static Font font_big;

    private static int frames = 0;
    private static long fps_timer = 0;
    private static boolean screenshot = false;

    public static boolean combat_menu_shown = false;

    public static int replayOption = 0;

    public static String[] shellStrings;

    private static boolean macOS_resize_workaround = Util.isMacOS();

    public static boolean quietScreenshot = false;


    public static void init() {
        // Resize game window
        new_size.width = 512;
        new_size.height = 357 - GAME_RENDER_OFFSET; // actual usable height for game content rendering
        handle_resize();

        // Load fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            InputStream is = Launcher.getResourceAsStream("/assets/Helvetica-Bold.ttf");
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            ge.registerFont(font);
            font_main = font.deriveFont(Font.PLAIN, 11.0f);
            font_big = font.deriveFont(Font.PLAIN, 22.0f);

            is = Launcher.getResourceAsStream("/assets/TimesRoman.ttf");
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, is));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load images
        try {
            image_border = ImageIO.read(Launcher.getResource("/assets/hbar/border.png"));
            image_bar_frame = ImageIO.read(Launcher.getResource("/assets/hbar/bar.png"));
            image_bar_frame_short = ImageIO.read(Launcher.getResource("/assets/hbar/bar_short.png"));
            image_wiki_hbar_inactive =
                    ImageIO.read(Launcher.getResource("/assets/hbar/wiki_hbar_inactive.png"));
            image_wiki_hbar_active =
                    ImageIO.read(Launcher.getResource("/assets/hbar/wiki_hbar_active.png"));
            image_cursor = ImageIO.read(Launcher.getResource("/assets/cursor.png"));
            image_highlighted_item = ImageIO.read(Launcher.getResource("/assets/highlighted_item.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void resize(int w, int h) {
        new_size.width = Math.max(w, 512);
        new_size.height = Math.max(h, 357 - GAME_RENDER_OFFSET); // actual height that could be used for game rendering
      }
    
    public static void handle_resize() {
        width = new_size.width;
        height = new_size.height;

        height_client = height - 12;
        pixels = new int[width * height];
        game_image = new BufferedImage(width, height + GAME_RENDER_OFFSET, BufferedImage.TYPE_INT_ARGB);

        Camera.resize();
        Menu.resize();

        if (image_consumer != null) image_consumer.setDimensions(width, height);

        if (Renderer.instance != null && Reflection.setGameBounds != null) {
          try {
            Reflection.setGameBounds.invoke(
                Renderer.instance, 0, 0, Renderer.width, Renderer.height);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    
    public static void present(Graphics g, Image image) {
    	// Update timing
        long new_time = System.currentTimeMillis();
        delta_time = (float) (new_time - time) / 1000.0f;
        time = new_time;
        alpha_time = 0.25f + (((float) Math.sin(time / 100) + 1.0f) / 2.0f * 0.75f);

        // This workaround is required to use custom resolution on macOS
        if (macOS_resize_workaround) {
          if (Settings.CUSTOM_CLIENT_SIZE.get(Settings.currentProfile)) {
            Game.getInstance().resizeFrameWithContents();
          } else {
            Game.getInstance().pack();
            Game.getInstance().setLocationRelativeTo(null);
          }
          macOS_resize_workaround = false;
        }

        // Reset dialogue option after force pressed in replay
        if (Replay.isPlaying && KeyboardHandler.dialogue_option != -1)
          KeyboardHandler.dialogue_option = -1;

        Graphics2D g2 =
            (Graphics2D) game_image.getGraphics(); // TODO: Declare g2 outside of the present method
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(font_main);
        
        g2.drawImage(image, 0, GAME_RENDER_OFFSET, null);
        int startingPixel = 512;
        //int startingPixel = Settings.PATCH_HBAR_512_LAST_PIXEL.get(Settings.currentProfile) ? 511 : 512;
        g2.drawImage(image_border, startingPixel, 0, width - startingPixel, 12, null);
        g2.drawImage(image_border, startingPixel, (height + GAME_RENDER_OFFSET) - 12, width - startingPixel, 12, null);

        // In-game UI
        if (Client.state == Client.STATE_GAME) {
        /*if (Client.state == Client.STATE_GAME) {
          int npcCount = 0;
          int playerCount = 0;

          // Update player coords
          for (Iterator<NPC> iterator = Client.npc_list.iterator(); iterator.hasNext(); ) {
            NPC npc = iterator.next(); // TODO: Remove unnecessary allocations
            if (npc != null) {
              if (npc.type == NPC.TYPE_PLAYER) playerCount++;
              else if (npc.type == NPC.TYPE_MOB) npcCount++;

              if (Client.player_name.equals(npc.name)) {
                Client.player_posX = npc.x;
                Client.player_posY = npc.y;
                Client.player_height = npc.height;
                Client.player_width = npc.width;

                Client.isGameLoaded = true;
              }
            }
          }

          if (!Client.isInterfaceOpen() && Client.show_menu == Client.MENU_NONE) {
            List<Rectangle> npc_hitbox = new ArrayList<>();
            List<Rectangle> player_hitbox = new ArrayList<>();
            List<Point> entity_text_loc = new ArrayList<>();

            for (Iterator<NPC> iterator = Client.npc_list.iterator(); iterator.hasNext(); ) {
              NPC npc = iterator.next(); // TODO: Remove unnecessary allocations
              Color color = color_low;

              boolean showName = false;
              if (npc.type == NPC.TYPE_PLAYER) {
                color = color_fatigue;

                if (Client.isFriend(npc.name)) {
                  color = color_hp;
                  if ((Settings.SHOW_FRIEND_NAME_OVERLAY.get(Settings.currentProfile)
                      || Settings.SHOW_PLAYER_NAME_OVERLAY.get(Settings.currentProfile))) {
                    showName = true;
                  }
                } else if (Settings.SHOW_PLAYER_NAME_OVERLAY.get(Settings.currentProfile)) {
                  showName = true;
                }
              } else if (npc.type == NPC.TYPE_MOB
                  && Settings.SHOW_NPC_NAME_OVERLAY.get(Settings.currentProfile)) {
                showName = true;
              }

              if (Settings.SHOW_HITBOX.get(Settings.currentProfile)) {
                List<Rectangle> hitbox = player_hitbox;
                boolean showHitbox = true;

                if (npc.type == NPC.TYPE_MOB) hitbox = npc_hitbox;

                for (Iterator<Rectangle> boxIterator = hitbox.iterator(); boxIterator.hasNext(); ) {
                  Rectangle rect = boxIterator.next(); // TODO: Remove unnecessary allocations
                  if (rect.x == npc.x
                      && rect.y == npc.y
                      && rect.width == npc.width
                      && rect.height == npc.height) {
                    showHitbox = false;
                    break;
                  }
                }

                if (showHitbox) {
                  setAlpha(g2, 0.3f);
                  g2.setColor(color);
                  g2.fillRect(npc.x, npc.y, npc.width, npc.height);
                  g2.setColor(Color.BLACK);
                  g2.drawRect(npc.x, npc.y, npc.width, npc.height);
                  setAlpha(g2, 1.0f);
                  hitbox.add(new Rectangle(npc.x, npc.y, npc.width, npc.height));
                }
              }

              // draw npc's name
              if (showName && npc.name != null) {
                int x = npc.x + (npc.width / 2);
                int y = npc.y - 20;
                for (Iterator<Point> locIterator = entity_text_loc.iterator();
                    locIterator.hasNext(); ) {
                  Point loc = locIterator.next(); // TODO: Remove unnecessary allocations
                  if (loc.x == x && loc.y == y) y -= 12;
                }

                String text = npc.name;
                if (Settings.EXTEND_IDS_OVERLAY.get(Settings.currentProfile)) {
                  text += (" (" + npc.id + "-" + npc.id2 + ")");
                }
                drawShadowText(g2, text, x, y, color, true);
                entity_text_loc.add(new Point(x, y));
              }
            }

            List<Rectangle> item_hitbox = new ArrayList<>();
            List<Point> item_text_loc = new ArrayList<>();

            if (Settings.SHOW_ITEM_GROUND_OVERLAY.get(
                Settings.currentProfile)) { // Don't sort if we aren't displaying any item names anyway
              try {
                // Keep items in (technically reverse) alphabetical order for SHOW_ITEMINFO instead of
                // randomly
                // changing places each frame
                Collections.sort(Client.item_list, new ItemComparator());
              } catch (Exception e) {
                // Sometimes Java helpfully complains that the sorting method violates its general
                // contract.
                e.printStackTrace();
              }
            }

            for (Iterator<Item> iterator = Client.item_list.iterator(); iterator.hasNext(); ) {
              Item item = iterator.next(); // TODO: Remove unnecessary allocations

              if (Settings.SHOW_HITBOX.get(Settings.currentProfile)) {
                boolean show = true;
                for (Iterator<Rectangle> boxIterator = item_hitbox.iterator();
                    boxIterator.hasNext(); ) {
                  Rectangle rect = boxIterator.next(); // TODO: Remove unnecessary allocations
                  if (rect.x == item.x
                      && rect.y == item.y
                      && rect.width == item.width
                      && rect.height == item.height) {
                    show = false;
                    break;
                  }
                }

                if (show) {
                  setAlpha(g2, 0.3f);
                  g2.setColor(color_prayer);
                  g2.fillRect(item.x, item.y, item.width, item.height);
                  g2.setColor(Color.BLACK);
                  g2.drawRect(item.x, item.y, item.width, item.height);
                  setAlpha(g2, 1.0f);
                  item_hitbox.add(new Rectangle(item.x, item.y, item.width, item.height));
                }
              }

              if (Settings.SHOW_ITEM_GROUND_OVERLAY.get(Settings.currentProfile)) {
                int x = item.x + (item.width / 2);
                int y = item.y - 20;
                int freq = Collections.frequency(Client.item_list, item);

                // Check if item is in blocked list
                boolean itemIsBlocked =
                    stringIsWithinList(item.getName(), Settings.BLOCKED_ITEMS.get("custom"));

                // We've sorted item list in such a way that it is possible to not draw the ITEMINFO
                // unless it's the first time we've tried to for this itemid at that location
                // by just using last_item.
                // last_item == null necessary in case only one item on screen is being rendered. slight
                // speed increase from freq == 1 if compiler can stop early in conditional.
                if ((freq == 1 || !item.equals(last_item) || last_item == null) && !itemIsBlocked) {
                  for (Iterator<Point> locIterator = item_text_loc.iterator();
                      locIterator.hasNext(); ) {
                    Point loc = locIterator.next(); // TODO: Remove unnecessary allocations
                    if (loc.x == x && loc.y == y) {
                      y -= 12;
                    }
                  }
                  item_text_loc.add(new Point(x, y));

                  Color itemColor = color_item;
                  String itemText = item.getName() + ((freq == 1) ? "" : " (" + freq + ")");

                  // Check if item is in highlighted list
                  if (stringIsWithinList(item.getName(), Settings.HIGHLIGHTED_ITEMS.get("custom"))) {
                    itemColor = color_item_highlighted;
                    drawHighlighImage(g2, itemText, x, y);
                  }

                  // Note that it is not possible to show how many of a
                  //   stackable item are in a stack on the ground.
                  // That information is not transmitted in RSC, just that the item ID is there.
                  drawShadowText(g2, itemText, x, y, itemColor, true);
                }
                last_item = item; // Done with item this loop, can save it as last_item
              }
            }
          }

          // Clear item list for next frame
          Client.item_list.clear();
          last_item = null;

          if (!Client.show_sleeping && Settings.SHOW_INVCOUNT.get(Settings.currentProfile))
            drawShadowText(
                g2,
                Client.inventory_count + "/" + Client.max_inventory,
                width - 19,
                17,
                color_text,
                true);

          int percentHP = 0;
          int percentPrayer = 0;
          float alphaHP = 1.0f;
          float alphaPrayer = 1.0f;
          float alphaFatigue = 1.0f;
          Color colorHP = color_hp;
          Color colorPrayer = color_prayer;
          Color colorFatigue = color_fatigue;

          if (Client.getBaseLevel(Client.SKILL_HP) > 0) {
            percentHP =
                Client.getCurrentLevel(Client.SKILL_HP) * 100 / Client.getBaseLevel(Client.SKILL_HP);
            percentPrayer =
                Client.getCurrentLevel(Client.SKILL_PRAYER)
                    * 100
                    / Client.getBaseLevel(Client.SKILL_PRAYER);
          }

          if (percentHP < 30) {
            colorHP = color_low;
            alphaHP = alpha_time;
          }

          if (percentPrayer < 30) {
            colorPrayer = color_low;
            alphaPrayer = alpha_time;
          }

          if (Client.getFatigue() >= 80) {
            colorFatigue = color_low;
            alphaFatigue = alpha_time;
          }

          // Low HP notification
          if (percentHP <= Settings.LOW_HP_NOTIF_VALUE.get(Settings.currentProfile)
              && lastPercentHP > percentHP
              && lastPercentHP > Settings.LOW_HP_NOTIF_VALUE.get(Settings.currentProfile))
            NotificationsHandler.notify(
                NotifType.LOWHP, "Low HP Notification", "Your HP is at " + percentHP + "%");
          lastPercentHP = percentHP;

          // High fatigue notification
          if (Client.getFatigue() >= Settings.FATIGUE_NOTIF_VALUE.get(Settings.currentProfile)
              && lastFatigue < Client.getFatigue()
              && lastFatigue < Settings.FATIGUE_NOTIF_VALUE.get(Settings.currentProfile))
            NotificationsHandler.notify(
                NotifType.FATIGUE,
                "High Fatigue Notification",
                "Your fatigue is at " + Client.getFatigue() + "%");
          lastFatigue = Client.getFatigue();

          // Draw HP, Prayer, Fatigue overlay
          int x = 24;
          int y = 28;

          // combat menu is showing, so move everything down
          if (combat_menu_shown) y = 132;

          // NPC Post-processing for ui
          if (Settings.SHOW_COMBAT_INFO.get(Settings.currentProfile) && !Client.isInterfaceOpen()) {
            int bar_count = 0;
            for (Iterator<NPC> iterator = Client.npc_list.iterator(); iterator.hasNext(); ) {
              NPC npc = iterator.next();
              if (npc != null && Client.isInCombatWithNPC(npc)) {
                drawNPCBar(g2, 7, y, npc);
                // Increment y by npc bar height, so we can have multiple bars
                // NOTE: We should never (?) have more than one npc health bar, so multiple bars
                // indicates that our combat detection isn't accurate
                y += 50;
                bar_count++;
              }
            }
            if (bar_count > 0) {
              y += 16;
            }
          }

          if (Settings.SHOW_HP_PRAYER_FATIGUE_OVERLAY.get(Settings.currentProfile)) {
            if (roomInHbarForHPPrayerFatigueOverlay()) {
              if (!Client.isInterfaceOpen() && !Client.show_questionmenu) {
                setAlpha(g2, alphaHP);
                drawShadowText(
                    g2,
                    "Hits: "
                        + Client.current_level[Client.SKILL_HP]
                        + "/"
                        + Client.base_level[Client.SKILL_HP],
                    x,
                    y,
                    colorHP,
                    false);
                y += 16;
                setAlpha(g2, alphaPrayer);
                drawShadowText(
                    g2,
                    "Prayer: "
                        + Client.current_level[Client.SKILL_PRAYER]
                        + "/"
                        + Client.base_level[Client.SKILL_PRAYER],
                    x,
                    y,
                    colorPrayer,
                    false);
                y += 16;
                setAlpha(g2, alphaFatigue);
                drawShadowText(
                    g2, "Fatigue: " + Client.getFatigue() + "/100", x, y, colorFatigue, false);
                setAlpha(g2, 1.0f);
                y += 16;
              }
            } else {
              int barSize = 4 + image_bar_frame.getWidth(null);
              int x2 = width - (4 + barSize);
              int y2 = height - image_bar_frame.getHeight(null);

              drawBar(
                  g2, image_bar_frame, x2, y2, colorFatigue, alphaFatigue, Client.getFatigue(), 100);
              x2 -= barSize;

              drawBar(
                  g2,
                  image_bar_frame,
                  x2,
                  y2,
                  colorPrayer,
                  alphaPrayer,
                  Client.current_level[Client.SKILL_PRAYER],
                  Client.base_level[Client.SKILL_PRAYER]);
              x2 -= barSize;

              drawBar(
                  g2,
                  image_bar_frame,
                  x2,
                  y2,
                  colorHP,
                  alphaHP,
                  Client.current_level[Client.SKILL_HP],
                  Client.base_level[Client.SKILL_HP]);
              x2 -= barSize;
            }
          }

          // Draw last menu action
          if (!Client.isInterfaceOpen()
              && !Client.show_questionmenu
              && Settings.SHOW_LAST_MENU_ACTION.get(Settings.currentProfile)) {
            if (time <= Client.menu_timer) {
              drawShadowText(g2, Client.lastAction, x, y, color_text, false);
              y += 14;
            }
          }

          // Draw under combat style info
          // buffs, debuffs and cooldowns
          if (!Client.isInterfaceOpen() && Settings.SHOW_BUFFS.get(Settings.currentProfile)) {
            if (time <= Client.magic_timer) {
              float timer = (float) Math.ceil((Client.magic_timer - time) / 1000.0);
              drawShadowText(g2, "Magic Timer: " + (int) timer, x, y, color_text, false);
              y += 14;
            }

            for (int i = 0; i < 18; i++) {
              if (Client.current_level[i] != Client.base_level[i]
                  && (i != Client.SKILL_HP && i != Client.SKILL_PRAYER)) {
                int diff = Client.current_level[i] - Client.base_level[i];
                Color color = color_low;

                // Build our boost string
                // If the difference is greater than 0 (positive boost), we need to add a "+" to the
                // string
                // Otherwise, it can be left alone because the integer will already have a "-" when
                // converted to a string
                String boost = Integer.toString(diff);
                if (diff > 0) {
                  boost = "+" + boost;
                  color = color_hp;
                }

                drawShadowText(g2, boost, x, y, color, false);
                drawShadowText(g2, Client.skill_name[i], x + 32, y, color, false);
                y += 14;
              }
            }

            int base_drain_rate = 0;
            float adjusted_drain_rate = 0;
            // 14 selectable prayers
            for (int i = 0; i < 14; i++) {
              if (Client.prayers_on[i] == true) base_drain_rate += Client.DRAIN_RATES[i];
            }
            lastBaseDrainRate = base_drain_rate;
            if (base_drain_rate != 0) {
              // with prayer equipment, combat rounds get increased about 3.1% per +1
              float factor = 1.0f;
              if (Client.current_equipment_stats[4] > 1) {
                int boost = Client.current_equipment_stats[4] - 1;
                float increase_rounds = boost * 0.031f;
                // percentage of increase per round
                factor = 1 + increase_rounds;
              }
              adjusted_drain_rate = base_drain_rate / factor;
              lastAdjustedDrainRate = adjusted_drain_rate;
              // a drain_rate of 60 drains 1 point in 3.33 secs
              // 75 drains 1.25 points in 3.33 secs -> 3.33/(adjusted/60)
              float points_psec = (3.33f * 60.0f) / adjusted_drain_rate;

              drawShadowText(g2, "-1", x, y, color_low, false);
              drawShadowText(
                  g2, "Prayer/" + Client.trimNumber(points_psec, 1) + "s", x + 32, y, color_low, false);
              y += 14;
            } else {
              // no prayer armour adjusting drain rate
              lastAdjustedDrainRate = lastBaseDrainRate;
            }
            if (time > Client.poison_timer && Client.is_poisoned) {
              // more than 20 seconds passed and last status was poison, user probably is no longer
              // poisoned
              Client.is_poisoned = false;
              Client.poison_timer = time;
            }
            if (Client.is_poisoned) {
              drawShadowText(g2, "Poisoned!", x, y, color_poison, false);
              y += 14;
            }
          }

          // Clear npc list for the next frame
          Client.npc_list.clear();

          // render XP bar/drop
          Client.processFatigueXPDrops();
          Client.xpdrop_handler.draw(g2);
          Client.xpbar.draw(g2);

          if (!Client.isSleeping()) {
            Client.updateCurrentFatigue();
          }

          // Make the reset buttons for filter & sort flash red a few frames when clicked
          if (Bank.buttonActive[5]
              || Bank.buttonActive[11]
              || (Bank.buttonActive[10] || Bank.disableUserButton)) {
            if (bankResetTimer > 3) {
              if (Bank.disableUserButton) {
                Bank.buttonActive[10] = false;
                Bank.disableUserButton = false;
              } else {
                Bank.buttonActive[5] = false;
                Bank.buttonActive[11] = false;
              }
              bankResetTimer = 0;
            } else {
              ++bankResetTimer;
            }
          }
          // Handles drawing bank value, bank sort panel, & bank filter panel
          Bank.drawBankAugmentations(g2);

          // World Map
          // Arrow marker for destination
          if (WorldMapWindow.getWaypointPosition() != null
              && !Client.isInterfaceOpen()
              && Client.show_menu == Client.MENU_NONE) {
            float absCameraRotation =
                ((((float) Camera.rotation / 255.0f) * 360.0f) + 180.0f)
                    + WorldMapWindow.getWaypointAngle();
            if (WorldMapWindow.getWaypointFloor() != Client.planeIndex) setAlpha(g2, 0.5f);
            Image arrowSprite = WorldMapWindow.directions[Util.getAngleIndex(absCameraRotation)];
            x = (width / 2);
            y = 54;
            g2.drawImage(arrowSprite, x - 12, y - 12, 24, 24, null);
            y += -20;
            drawShadowText(
                g2,
                "" + Util.getAngleDirectionName(WorldMapWindow.getWaypointAngle()),
                x,
                y,
                Renderer.color_low,
                true);
            setAlpha(g2, 1.0f);
          }

          // RSC Wiki integration
          if (WikiURL.nextClickIsLookup && MouseHandler.mouseClicked) {
            if (MouseHandler.rightClick) {
              WikiURL.nextClickIsLookup = false;
              Client.displayMessage("Cancelled lookup.", Client.CHAT_NONE);
            } else {
              if (System.currentTimeMillis() - WikiURL.lastLookupTime > WikiURL.cooldownTimer) {
                if (!MouseText.isPlayer) {
                  String wikiURL = WikiURL.translateNameToUrl(MouseText.name);
                  if (!wikiURL.equals("INVALID")) {
                    WikiURL.lastLookupTime = System.currentTimeMillis();
                    Client.displayMessage(
                        "@whi@Looking up @gre@" + MouseText.name + "@whi@ on the wiki!",
                        Client.CHAT_NONE);
                    Util.openLinkInBrowser(wikiURL);
                    WikiURL.nextClickIsLookup = false;
                  }
                } else {
                  Client.displayMessage(
                      "@lre@Players cannot be looked up on the wiki, try again.", Client.CHAT_NONE);
                }
              } else {
                Client.displayMessage(
                    String.format(
                        "@lre@Please wait %1d seconds between wiki queries",
                        WikiURL.cooldownTimer / 1000),
                    Client.CHAT_NONE);
              }
            }
          }

          if (Settings.WIKI_LOOKUP_ON_HBAR.get(Settings.currentProfile)) {
            int xCoord = Client.wikiLookupReplacesReportAbuse() ? 410 : 410 + 90 + 12;
            int yCoord = height - 16;
            // Handle replay play selection click
            if (MouseHandler.x >= xCoord + 3 // + 3 for hbar shadow included in image
                && MouseHandler.x <= xCoord + 3 + 90
                && MouseHandler.y >= height - 16
                && MouseHandler.y <= height
                && MouseHandler.mouseClicked) {
              Client.displayMessage(
                  "Click on something to look it up on the wiki...", Client.CHAT_NONE);
              WikiURL.nextClickIsLookup = true;
            }
            if (WikiURL.nextClickIsLookup) {
              g2.drawImage(image_wiki_hbar_active, xCoord, yCoord, 96, 15, null);
            } else {
              g2.drawImage(image_wiki_hbar_inactive, xCoord, yCoord, 96, 15, null);
            }
          }

          // Interface rsc+ buttons
          // Map Button
          if (Settings.RSCPLUS_BUTTONS_FUNCTIONAL.get(Settings.currentProfile)
              || Settings.SHOW_RSCPLUS_BUTTONS.get(Settings.currentProfile)) {
            Rectangle mapButtonBounds = new Rectangle(width - 68, 3, 32, 32);
            if ((!Client.show_bank || mapButtonBounds.x >= 460) && !Client.show_sleeping) {
              if (Settings.SHOW_RSCPLUS_BUTTONS.get(Settings.currentProfile)) {
                g2.setColor(Renderer.color_text);
                g2.drawLine(
                    mapButtonBounds.x + 4,
                    mapButtonBounds.y + 1,
                    mapButtonBounds.x + 4,
                    mapButtonBounds.y + 1 + 6);
                g2.drawLine(
                    mapButtonBounds.x + 1,
                    mapButtonBounds.y + 4,
                    mapButtonBounds.x + 7,
                    mapButtonBounds.y + 4);
              }

              // Handle replay play selection click
              if (MouseHandler.x >= mapButtonBounds.x
                  && MouseHandler.x <= mapButtonBounds.x + mapButtonBounds.width
                  && MouseHandler.y >= mapButtonBounds.y
                  && MouseHandler.y <= mapButtonBounds.y + mapButtonBounds.height
                  && MouseHandler.mouseClicked) {

                Launcher.getWorldMapWindow().showWorldMapWindow();
              }
            }

            // Settings
            mapButtonBounds = new Rectangle(width - 200, 3, 32, 32);
            if ((!Client.show_bank || mapButtonBounds.x >= 460) && !Client.show_sleeping) {
              if (Settings.SHOW_RSCPLUS_BUTTONS.get(Settings.currentProfile)) {
                g2.setColor(Renderer.color_text);
                g2.drawLine(
                    mapButtonBounds.x + 4,
                    mapButtonBounds.y + 1,
                    mapButtonBounds.x + 4,
                    mapButtonBounds.y + 1 + 6);
                g2.drawLine(
                    mapButtonBounds.x + 1,
                    mapButtonBounds.y + 4,
                    mapButtonBounds.x + 7,
                    mapButtonBounds.y + 4);
              }

              // Handle replay play selection click
              if (MouseHandler.x >= mapButtonBounds.x
                  && MouseHandler.x <= mapButtonBounds.x + mapButtonBounds.width
                  && MouseHandler.y >= mapButtonBounds.y
                  && MouseHandler.y <= mapButtonBounds.y + mapButtonBounds.height
                  && MouseHandler.mouseClicked) {

                Launcher.getConfigWindow().showConfigWindow();
              }
            }

            // wiki button on magic book (Probably a bad idea due to misclicks)
            if (Settings.WIKI_LOOKUP_ON_MAGIC_BOOK.get(Settings.currentProfile)) {
              mapButtonBounds = new Rectangle(width - 68 - 66, 3, 32, 32);
              if ((!Client.show_bank || mapButtonBounds.x >= 460) && !Client.show_sleeping) {
                if (Settings.SHOW_RSCPLUS_BUTTONS.get(Settings.currentProfile)) {
                  g2.setColor(Renderer.color_text);
                  g2.drawLine(
                      mapButtonBounds.x + 4,
                      mapButtonBounds.y + 1,
                      mapButtonBounds.x + 4,
                      mapButtonBounds.y + 1 + 6);
                  g2.drawLine(
                      mapButtonBounds.x + 1,
                      mapButtonBounds.y + 4,
                      mapButtonBounds.x + 7,
                      mapButtonBounds.y + 4);
                }

                // Handle replay play selection click
                if (MouseHandler.x >= mapButtonBounds.x
                    && MouseHandler.x <= mapButtonBounds.x + mapButtonBounds.width
                    && MouseHandler.y >= mapButtonBounds.y
                    && MouseHandler.y <= mapButtonBounds.y + mapButtonBounds.height
                    && MouseHandler.mouseClicked) {
                  Client.displayMessage(
                      "Click on something to look it up on the wiki...", Client.CHAT_NONE);
                  WikiURL.nextClickIsLookup = true;
                }
              }
            }
          }

          if (Settings.DEBUG.get(Settings.currentProfile)) {
            x = 32;
            y = 32;

            // Draw Skills
            for (int i = 0; i < 18; i++) {
              drawShadowText(
                  g2,
                  Client.skill_name[i]
                      + " ("
                      + i
                      + "): "
                      + Client.current_level[i]
                      + "/"
                      + Client.base_level[i]
                      + " ("
                      + Client.getXP(i)
                      + " xp)",
                  x,
                  y,
                  color_text,
                  false);
              y += 16;
            }

            // Draw Fatigue
            y += 16;
            drawShadowText(
                g2, "Fatigue: " + ((float) Client.fatigue * 100.0f / 750.0f), x, y, color_text, false);
            y += 16;

            // Draw Drain rates
            y += 16;
            drawShadowText(g2, "Base Drain Rate: " + lastBaseDrainRate, x, y, color_text, false);
            y += 16;
            drawShadowText(
                g2,
                "Adjusted Drain Rate: " + Client.trimNumber(lastAdjustedDrainRate, 1),
                x,
                y,
                color_text,
                false);
            y += 16;

            // Draw Mouse Info
            y += 16;
            drawShadowText(
                g2,
                "Mouse Position: " + MouseHandler.x + ", " + MouseHandler.y,
                x,
                y,
                color_text,
                false);
            y += 16;

            // Draw camera info
            y += 16;
            drawShadowText(g2, "Camera Rotation: " + Camera.rotation, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Angle: " + Camera.angle, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Auto: " + Camera.auto, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Auto Speed: " + Camera.auto_speed, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Rotation Y: " + Camera.rotation_y, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Lookat X: " + Camera.lookat_x, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Lookat Y: " + Camera.lookat_y, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Zoom: " + Camera.zoom, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Distance1: " + Camera.distance1, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Distance2: " + Camera.distance2, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Distance3: " + Camera.distance3, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Camera Distance4: " + Camera.distance4, x, y, color_text, false);
            y += 16;

            x = 256;
            y = 32;
            drawShadowText(
                g2, "FPS: " + fps + " (" + Client.updatesPerSecond + ")", x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "FPS Sleepiness: " + sleepTimer, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Interlace: " + Client.getInterlace(), x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Game Size: " + width + "x" + height, x, y, color_text, false);
            y += 16;

            // Draw Inventory items
            y += 16;
            for (int i = 0; i < Client.inventory_count; i++) {
              drawShadowText(g2, "(" + i + "): " + Client.inventory_items[i], x, y, color_text, false);
              y += 16;
            }

            y += 16;
            drawShadowText(g2, "Menu: " + Client.show_menu, x, y, color_text, false);
            y += 16;

            x = 380;
            y = 32;
            drawShadowText(g2, Client.player_name, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Player Count: " + playerCount, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "NPC Count: " + npcCount, x, y, color_text, false);
            y += 16;
            drawShadowText(
                g2,
                "LocalRegion: (" + Client.localRegionX + "," + Client.localRegionY + ")",
                x,
                y,
                color_text,
                false);
            y += 16;
            drawShadowText(
                g2, "Region: (" + Client.regionX + "," + Client.regionY + ")", x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "WorldCoord: " + Client.getCoords(), x, y, color_text, false);
            y += 16;
            drawShadowText(
                g2,
                "Plane: ("
                    + Client.planeWidth
                    + ","
                    + Client.planeHeight
                    + ","
                    + Client.planeIndex
                    + ")",
                x,
                y,
                color_text,
                false);
            y += 16;
            drawShadowText(g2, "combat_timer: " + Client.combat_timer, x, y, color_text, false);
            y += 32;
            drawShadowText(g2, "frame_time_slice: " + Replay.frame_time_slice, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "lag: " + Replay.timestamp_lag + " updates", x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "replay_timestamp: " + Replay.timestamp, x, y, color_text, false);
            y += 16;
            drawShadowText(
                g2,
                "replay_server_timestamp: " + Replay.timestamp_server_last,
                x,
                y,
                color_text,
                false);
            y += 16;
            drawShadowText(
                g2, "replay_client_timestamp: " + Replay.timestamp_client, x, y, color_text, false);
            y += 16;
            drawShadowText(
                g2, "replay_client_read: " + Replay.getClientRead(), x, y, color_text, false);
            y += 16;
            drawShadowText(
                g2, "replay_client_write: " + Replay.getClientWrite(), x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Last sound effect: " + Client.lastSoundEffect, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Mouse Text: " + MouseText.mouseText, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Hover: " + Client.is_hover, x, y, color_text, false);
            y += 16;
            drawShadowText(g2, "Java version: " + Settings.javaVersion, x, y, color_text, false);
          }

          // A little over a full tick
          int threshold = 35;

          if (Replay.isPlaying && Replay.fpsPlayMultiplier > 1.0)
            threshold = 35 * 3; // this is to prevent blinking during fastforward

          if (Settings.LAG_INDICATOR.get(Settings.currentProfile)
              && Replay.getServerLag() >= threshold) {
            x = width - 80;
            y = height - 80;
            setAlpha(g2, alpha_time);
            g2.drawImage(Launcher.icon_warn.getImage(), x, y, 32, 32, null);
            x += 16;
            y += 38;
            drawShadowText(g2, "Server Lag", x, y, color_fatigue, true);
            y += 12;
            int lag = (Replay.getServerLag() - 31) * Replay.getFrameTimeSlice();
            drawShadowText(
                g2,
                new DecimalFormat("0.0").format((float) lag / 1000.0f) + "s",
                x,
                y,
                color_low,
                true);
            setAlpha(g2, 1.0f);
          }
          if (!(Replay.isPlaying && !Settings.TRIGGER_ALERTS_REPLAY.get(Settings.currentProfile))) {
            g2.setFont(font_big);
            if (Settings.FATIGUE_ALERT.get(Settings.currentProfile)
                && Client.getFatigue() >= 98
                && !Client.isInterfaceOpen()) {
              setAlpha(g2, alpha_time);
              drawShadowText(g2, "FATIGUED", width / 2, height / 2, color_low, true);
              setAlpha(g2, 1.0f);
            }
            if (Settings.INVENTORY_FULL_ALERT.get(Settings.currentProfile)
                && Client.inventory_count >= 30
                && !Client.isInterfaceOpen()) {
              setAlpha(g2, alpha_time);
              drawShadowText(g2, "INVENTORY FULL", width / 2, height / 2, color_low, true);
              setAlpha(g2, 1.0f);
            }
            g2.setFont(font_main);
          }*/

          // TODO: needs to be placed as overlay config
          if (true /*Settings.SHOW_PLAYER_POSITION.get(Settings.currentProfile)*/) {
            int y = Renderer.height - 11;
            int offset = 0;
            /*if (Replay.isPlaying) {
              if ((!screenshot && Settings.SHOW_SEEK_BAR.get(Settings.currentProfile))) y -= 12;
            }*/
            if ((!Replay.isPlaying || screenshot)) offset += 50;
            drawShadowText(
                g2,
                "Pos: " + Client.getCoords(),
                (Renderer.width - 92 - offset),
                y,
                color_yellow,
                false);
          }

          // Mouseover hover handling
          /*if (Settings.SHOW_MOUSE_TOOLTIP.get(Settings.currentProfile)
              && !Client.isInterfaceOpen()
              && !Client.show_questionmenu
              && Client.is_hover) {
            final int extraOptionsOffsetX = 8;
            final int extraOptionsOffsetY = 12;

            x = MouseHandler.x + 16;
            y = MouseHandler.y + 28;

            // Dont allow text to go off the screen
            Dimension bounds = getStringBounds(g2, MouseText.colorlessText);
            Dimension extraBounds = getStringBounds(g2, MouseText.extraOptions);
            if (MouseText.extraOptions.length() == 0) extraBounds.height = 0;

            bounds.height += extraOptionsOffsetY;
            if (Settings.SHOW_EXTENDED_TOOLTIP.get(Settings.currentProfile)) {
              extraBounds.width += extraOptionsOffsetX;
              bounds.width = (bounds.width > extraBounds.width) ? bounds.width : extraBounds.width;
              bounds.height += extraBounds.height;
            }
            if (x + bounds.width > Renderer.width - 4) x -= (x + bounds.width) - (Renderer.width - 4);
            if (y + bounds.height > Renderer.height) y -= (y + bounds.height) - (Renderer.height);

            // Draw the final outcome
            if (Settings.SHOW_EXTENDED_TOOLTIP.get(Settings.currentProfile)) {
              setAlpha(g2, 0.65f);
              g2.setColor(color_shadow);
              g2.fillRect(x - 4, y - 12, bounds.width + 8, bounds.height - 8);
              setAlpha(g2, 1.0f);
              drawColoredText(g2, MouseText.getMouseOverText(), x, y);
              x += extraOptionsOffsetX;
              y += extraOptionsOffsetY;
              drawColoredText(g2, "@whi@" + MouseText.extraOptions, x, y);
            } else {
              if (!MouseText.getMouseOverText().contains("Walk here")
                  && !MouseText.getMouseOverText().contains("Choose a target")) {
                setAlpha(g2, 0.65f);
                g2.setColor(color_shadow);
                g2.fillRect(x - 4, y - 12, bounds.width + 8, bounds.height - 8);
                setAlpha(g2, 1.0f);
                drawColoredText(g2, MouseText.getMouseOverText(), x, y);
              }
            }
          }*/
        } else if (Client.state == Client.STATE_LOGIN) {
          //if (Settings.DEBUG.get(Settings.currentProfile))
          //  drawShadowText(g2, "DEBUG MODE", 38, 8, color_text, true);

          // Draw world list
          drawShadowText(g2, "World (Click to change): ", 80, height - 8, color_text, true);
          for (int i = 1; i <= Settings.WORLDS_TO_DISPLAY; i++) {
            Rectangle bounds = new Rectangle(134 + (i * 18), height - 12, 16, 12);
            Color color = color_text;

            if (i == Settings.WORLD.get(Settings.currentProfile)) color = color_low;

            setAlpha(g2, 0.5f);
            g2.setColor(color);
            g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            setAlpha(g2, 1.0f);
            String worldString = Integer.toString(i);
            drawShadowText(
                g2, worldString, bounds.x + (bounds.width / 2), bounds.y + 4, color_text, true);

            // Handle world selection click
            if (MouseHandler.x >= bounds.x
                && MouseHandler.x <= bounds.x + bounds.width
                && MouseHandler.y >= bounds.y
                && MouseHandler.y <= bounds.y + bounds.height
                && MouseHandler.mouseClicked) {
              Game.getInstance().getJConfig().changeWorld(i);
            }
          }

          // draw server replay buttons
          boolean wantToDrawServerReplayButtons =
              Client.login_screen == Client.SCREEN_USERNAME_PASSWORD_LOGIN
                  || Client.login_screen == Client.SCREEN_CLICK_TO_LOGIN;
          if (wantToDrawServerReplayButtons) {
            // TODO: This will need to be adjusted when the login screen is resizable
            Rectangle recordButtonBounds;

            // TODO: if Lost Password button can be toggled separately, this should be changed
            boolean longForm = true;

            if (Client.login_screen == Client.SCREEN_CLICK_TO_LOGIN) {
              longForm = true;
            } else if (Client.login_screen == Client.SCREEN_USERNAME_PASSWORD_LOGIN) {
              longForm = true; //Client.loginLostPasswordButton == 0;
            }
            if (longForm) {
              if (Client.login_screen == Client.SCREEN_USERNAME_PASSWORD_LOGIN) {
                recordButtonBounds = new Rectangle(512 - 148, 346 - 36, 48, 16);
              } else if (Client.login_screen == Client.SCREEN_CLICK_TO_LOGIN) {
                recordButtonBounds = new Rectangle(512 - 140, 288, 48, 16);
              } else {
                Logger.Error("Specify -server replay- bounds for this screen!");
                // original bounds, underneath the Cancel button
                recordButtonBounds = new Rectangle(512 - 148, 346 - 36, 48, 16);
              }
              drawShadowText(
                  g2,
                  "-server replay-",
                  recordButtonBounds.x + 48,
                  recordButtonBounds.y - 10,
                  color_fatigue,
                  true);
            } else {
              if (Client.login_screen == Client.SCREEN_USERNAME_PASSWORD_LOGIN) {
                recordButtonBounds = new Rectangle(512 - 33, 250 - 12, 25, 25);
                // bounds = new Rectangle(512 - 148, 346 - 36, 48, 16);
              } else {
                Logger.Error("Specify tiny -server replay- bounds for this screen!");
                // original bounds, underneath the Cancel button
                recordButtonBounds = new Rectangle(512 - 148, 346 - 36, 48, 16);
              }
            }

            setAlpha(g2, 0.5f);
            /*if (Settings.SPEEDRUNNER_MODE_ACTIVE.get(Settings.currentProfile)) {
              g2.setColor(color_hp);
            } else {
              if (replayOption == 1 || Settings.RECORD_AUTOMATICALLY.get(Settings.currentProfile)) {
                g2.setColor(color_low);
              } else {
                g2.setColor(color_text);
              }
            }*/
            if (longForm) {
              g2.fillRect(
                  recordButtonBounds.x,
                  recordButtonBounds.y,
                  recordButtonBounds.width,
                  recordButtonBounds.height);
            } else {
              g2.fillOval(
                  recordButtonBounds.x,
                  recordButtonBounds.y,
                  recordButtonBounds.width,
                  recordButtonBounds.height);
            }

            /*if (Settings.RECORD_AUTOMATICALLY.get(Settings.currentProfile)) {
              g2.setColor(color_text);
              if (longForm) {
                g2.drawRect(
                    recordButtonBounds.x,
                    recordButtonBounds.y,
                    recordButtonBounds.width,
                    recordButtonBounds.height);
              } else {
                g2.drawOval(
                    recordButtonBounds.x,
                    recordButtonBounds.y,
                    recordButtonBounds.width,
                    recordButtonBounds.height);
              }
            }*/
            setAlpha(g2, 1.0f);
            String recordButtonText = "";
            /*if (longForm) {
              if (Settings.SPEEDRUNNER_MODE_ACTIVE.get(Settings.currentProfile)) {
                recordButtonText = "speedy";
              } else {
                recordButtonText = "record";
              }
            } else {
              // smaller buttons, less room for text
              if (Settings.SPEEDRUNNER_MODE_ACTIVE.get(Settings.currentProfile)) {
                recordButtonText = "run";
              } else {
                recordButtonText = "rec";
              }
            }*/
            drawShadowText(
                g2,
                recordButtonText,
                recordButtonBounds.x + (recordButtonBounds.width / 2) + 1,
                recordButtonBounds.y + (longForm ? 6 : 10),
                color_text,
                true);

            // Handle replay record selection click
            if (MouseHandler.x >= recordButtonBounds.x
                && MouseHandler.x <= recordButtonBounds.x + recordButtonBounds.width
                && MouseHandler.y >= recordButtonBounds.y
                && MouseHandler.y <= recordButtonBounds.y + recordButtonBounds.height
                && MouseHandler.mouseClicked) {
              //Client.showRecordAlwaysDialogue = true;

              if (replayOption == 1) {
                replayOption = 0;
              } else {
                replayOption = 1;
              }
            }
            Rectangle playButtonBounds;
            if (longForm) {
              playButtonBounds =
                  new Rectangle(
                      recordButtonBounds.x + recordButtonBounds.width + 4,
                      recordButtonBounds.y,
                      48,
                      recordButtonBounds.height);
            } else {
              playButtonBounds =
                  new Rectangle(recordButtonBounds.x - 4, recordButtonBounds.y + 33, 32, 19);
            }
            setAlpha(g2, 0.5f);
            if (replayOption == 2) g2.setColor(color_low);
            else g2.setColor(color_text);
            g2.fillRect(
                playButtonBounds.x,
                playButtonBounds.y,
                playButtonBounds.width,
                playButtonBounds.height);
            setAlpha(g2, 1.0f);
            drawShadowText(
                g2,
                "play",
                playButtonBounds.x + (playButtonBounds.width / 2) + 1,
                playButtonBounds.y + (longForm ? 6 : 7),
                color_text,
                true);

            // Handle replay play selection click
            /*if (MouseHandler.x >= playButtonBounds.x
                && MouseHandler.x <= playButtonBounds.x + playButtonBounds.width
                && MouseHandler.y >= playButtonBounds.y
                && MouseHandler.y <= playButtonBounds.y + playButtonBounds.height
                && MouseHandler.mouseClicked) {
              if (replayOption == 2) {
                replayOption = 0;
              } else {
                if (ReplayQueue.replayFileSelectAdd()) {
                  Renderer.replayOption = 2;
                  ReplayQueue.nextReplay();
                } else {
                  Renderer.replayOption = 0;
                }
              }
            }
          }*/

          // Draw version information
          drawShadowText(
              g2,
              "rscplus v" + String.format("%8.6f", Settings.VERSION_NUMBER),
              width - 164,
              height - 2,
              color_text,
              false);
        }

        if (Client.state == Client.STATE_GAME && Replay.isPlaying && !screenshot) {
          /*if (Settings.SHOW_SEEK_BAR.get(Settings.currentProfile)) {
            float percent = (float) Replay.timestamp / Replay.getReplayEnd();

            if (Replay.isSeeking) {
              percent = (float) Replay.getSeekEnd() / Replay.getReplayEnd();
            }

            // "extended" is "in hover mode"
            boolean extended = (MouseHandler.y >= height - 28);

            // draw shadow box while hovering
            if (extended) {
              Rectangle bounds = new Rectangle(0, height - 28, width, 48);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.75f);
              g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
              setAlpha(g2, 1.0f);
              g2.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            // Set small font
            g2.setFont(font_main);

            // Handle bar
            barBounds = new Rectangle(32, height - 27, width - 64, 8);
            g2.setColor(color_text);
            setAlpha(g2, 0.25f);
            g2.fillRect(barBounds.x, barBounds.y, barBounds.width, barBounds.height);
            g2.setColor(color_prayer);
            setAlpha(g2, 0.5f);
            g2.fillRect(
                barBounds.x, barBounds.y, (int) ((float) barBounds.width * percent), barBounds.height);
            g2.setColor(color_text);
            setAlpha(g2, 1.0f);
            g2.drawRect(
                barBounds.x, barBounds.y, (int) ((float) barBounds.width * percent), barBounds.height);
            g2.setColor(color_text);
            g2.drawRect(barBounds.x, barBounds.y, barBounds.width, barBounds.height);

            // flash the bar while paused
            if (Replay.paused) {
              g2.setColor(color_low);
              setAlpha(g2, alpha_time / 5.0f);
              g2.fillRect(barBounds.x, barBounds.y, barBounds.width, barBounds.height);
              setAlpha(g2, 1.0f);
            }

            // draw time-into-replay
            String elapsed =
                Util.formatTimeDuration(Replay.elapsedTimeMillis(), Replay.endTimeMillis());
            String end = Util.formatTimeDuration(Replay.endTimeMillis(), Replay.endTimeMillis());
            if (extended) {
              drawShadowText(
                  g2,
                  elapsed + " / " + end,
                  barBounds.x + (barBounds.width / 2),
                  barBounds.y + barBounds.height + 8,
                  color_replay,
                  true);
            }

            float percentClient = (float) Replay.getClientRead() / Replay.getClientWrite();
            int server_x = (int) (barBounds.width * percent);
            int client_x = (int) (server_x * percentClient);

            g2.setColor(color_prayer);
            setAlpha(g2, 0.5f);
            g2.fillRect(barBounds.x + 1, barBounds.y + 1, client_x - 1, barBounds.height - 1);

            if (MouseHandler.x >= barBounds.x
                && MouseHandler.x <= barBounds.x + barBounds.width
                && MouseHandler.y >= barBounds.y
                && MouseHandler.y <= barBounds.y + barBounds.height) {
              float percentEnd = (float) (MouseHandler.x - barBounds.x) / barBounds.width;
              int timestamp = (int) (Replay.getReplayEnd() * percentEnd);
              g2.setColor(color_fatigue);
              setAlpha(g2, 0.5f);
              g2.drawLine(MouseHandler.x, barBounds.y, MouseHandler.x, barBounds.y + barBounds.height);
              setAlpha(g2, 1.0f);
              drawShadowTextBorder(
                  g2,
                  Util.formatTimeDuration(timestamp * 20, Replay.endTimeMillis()),
                  MouseHandler.x,
                  barBounds.y - 8,
                  color_text,
                  1.0f,
                  0.75f,
                  true,
                  0);

              if (!Replay.isSeeking && MouseHandler.mouseClicked) Replay.seek(timestamp);
            }

            if (Replay.isSeeking) {
              drawShadowTextBorder(
                  g2,
                  "Seeking... Please wait",
                  barBounds.x + (barBounds.width / 2),
                  barBounds.y + barBounds.height - 18,
                  color_fatigue,
                  1.0f,
                  0.75f,
                  false,
                  2);
            }
            // draw & handle gui "video player" control buttons
            if (extended && Settings.SHOW_PLAYER_CONTROLS.get(Settings.currentProfile)) {
              final int BUTTON_WIDTH = 30;
              final int BUTTON_HEIGHT = 11;
              final int BUTTON_OFFSET_X = 4; // how many pixels between each button horizontally
              final int BUTTON_OFFSET_Y = 4; // how many pixels "down" from the bottom of
              // the seek bar to  draw the control buttons

              // buttons are in this order:
              // previous slowforward playpause fastforward next stop --- queue

              // previous button
              previousBounds =
                  new Rectangle(
                      barBounds.x,
                      barBounds.y + barBounds.height + BUTTON_OFFSET_Y,
                      BUTTON_WIDTH,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(
                  previousBounds.x, previousBounds.y, previousBounds.width, previousBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  previousBounds.x + 1,
                  previousBounds.y + 1,
                  previousBounds.width - 1,
                  previousBounds.height - 1);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              shapeHeight = previousBounds.height - 3;
              shapeX =
                  previousBounds.x
                      + (int) (((float) previousBounds.width) / 2.0)
                      - (int) ((float) shapeHeight / 2.0);
              drawPlayerControlShape(g2, shapeX, previousBounds.y + 2, shapeHeight, "previous");

              if (MouseHandler.inBounds(previousBounds) && MouseHandler.mouseClicked)
                Replay.controlPlayback("prev");

              // slowdown button
              slowForwardBounds =
                  new Rectangle(
                      previousBounds.x + previousBounds.width + BUTTON_OFFSET_X,
                      previousBounds.y,
                      BUTTON_WIDTH,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(
                  slowForwardBounds.x,
                  slowForwardBounds.y,
                  slowForwardBounds.width,
                  slowForwardBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  slowForwardBounds.x + 1,
                  slowForwardBounds.y + 1,
                  slowForwardBounds.width - 1,
                  slowForwardBounds.height - 1);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              shapeHeight = slowForwardBounds.height - 3;
              shapeX =
                  slowForwardBounds.x
                      + (int) (((float) slowForwardBounds.width) / 2.0)
                      - (int) ((float) shapeHeight / 2.0);
              drawPlayerControlShape(g2, shapeX, slowForwardBounds.y + 2, shapeHeight, "slowforward");

              if (MouseHandler.inBounds(slowForwardBounds) && MouseHandler.mouseClicked) {
                Replay.controlPlayback("ff_minus");
              }

              // play/pause (one button)
              playPauseBounds =
                  new Rectangle(
                      slowForwardBounds.x + slowForwardBounds.width + BUTTON_OFFSET_X,
                      previousBounds.y,
                      BUTTON_WIDTH,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(
                  playPauseBounds.x, playPauseBounds.y, playPauseBounds.width, playPauseBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  playPauseBounds.x + 1,
                  playPauseBounds.y + 1,
                  playPauseBounds.width - 1,
                  playPauseBounds.height - 1);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              shapeHeight = playPauseBounds.height - 3;
              shapeX =
                  playPauseBounds.x
                      + (int) (((float) playPauseBounds.width) / 2.0)
                      - (int) ((float) shapeHeight / 2.0);
              drawPlayerControlShape(g2, shapeX, playPauseBounds.y + 2, shapeHeight, "playpause");

              if (MouseHandler.inBounds(playPauseBounds) && MouseHandler.mouseClicked) {
                Replay.togglePause();
              }
              // fastforward button
              fastForwardBounds =
                  new Rectangle(
                      playPauseBounds.x + playPauseBounds.width + BUTTON_OFFSET_X,
                      previousBounds.y,
                      BUTTON_WIDTH,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(
                  fastForwardBounds.x,
                  fastForwardBounds.y,
                  fastForwardBounds.width,
                  fastForwardBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  fastForwardBounds.x + 1,
                  fastForwardBounds.y + 1,
                  fastForwardBounds.width - 1,
                  fastForwardBounds.height - 1);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              shapeHeight = fastForwardBounds.height - 3;
              shapeX =
                  fastForwardBounds.x
                      + (int) (((float) fastForwardBounds.width) / 2.0)
                      - (int) ((float) shapeHeight / 2.0);
              drawPlayerControlShape(g2, shapeX, fastForwardBounds.y + 2, shapeHeight, "fastforward");

              if (MouseHandler.inBounds(fastForwardBounds) && MouseHandler.mouseClicked) {
                Replay.controlPlayback("ff_plus");
              }

              // next button
              nextBounds =
                  new Rectangle(
                      fastForwardBounds.x + fastForwardBounds.width + BUTTON_OFFSET_X,
                      previousBounds.y,
                      BUTTON_WIDTH,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(nextBounds.x, nextBounds.y, nextBounds.width, nextBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  nextBounds.x + 1, nextBounds.y + 1, nextBounds.width - 1, nextBounds.height - 1);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              shapeHeight = nextBounds.height - 3;
              shapeX =
                  nextBounds.x
                      + (int) (((float) nextBounds.width) / 2.0)
                      - (int) ((float) shapeHeight / 2.0);
              drawPlayerControlShape(g2, shapeX, nextBounds.y + 2, shapeHeight, "next");

              if (MouseHandler.inBounds(nextBounds) && MouseHandler.mouseClicked)
                Replay.controlPlayback("next");

              // open queue button (right aligned)
              queueBounds =
                  new Rectangle(
                      barBounds.x + barBounds.width - BUTTON_WIDTH * 2,
                      previousBounds.y,
                      BUTTON_WIDTH * 2,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(queueBounds.x, queueBounds.y, queueBounds.width, queueBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  queueBounds.x + 1, queueBounds.y + 1, queueBounds.width - 1, queueBounds.height - 1);
              g2.setColor(color_text);
              setAlpha(g2, 1.0f);

              shapeHeight = queueBounds.height - 3;
              shapeX = queueBounds.x + 3;
              drawPlayerControlShape(g2, shapeX, queueBounds.y + 2, shapeHeight, "queue");

              drawShadowText(
                  g2,
                  "Queue",
                  queueBounds.x + BUTTON_OFFSET_X + (int) (shapeHeight * 2),
                  queueBounds.y + queueBounds.height - 2,
                  color_white,
                  false);

              if (MouseHandler.inBounds(queueBounds) && MouseHandler.mouseClicked) {
                Launcher.getQueueWindow().showQueueWindow();
              }

              // stop button
              stopBounds =
                  new Rectangle(
                      queueBounds.x - BUTTON_WIDTH - BUTTON_OFFSET_X,
                      previousBounds.y,
                      BUTTON_WIDTH,
                      BUTTON_HEIGHT);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              g2.drawRect(stopBounds.x, stopBounds.y, stopBounds.width, stopBounds.height);
              g2.setColor(color_shadow);
              setAlpha(g2, 0.50f);
              g2.fillRect(
                  stopBounds.x + 1, stopBounds.y + 1, stopBounds.width - 1, stopBounds.height - 1);

              g2.setColor(color_text);
              setAlpha(g2, 1.0f);
              shapeHeight = stopBounds.height - 3;
              shapeX =
                  stopBounds.x
                      + (int) (((float) stopBounds.width) / 2.0)
                      - (int) ((float) shapeHeight / 2.0);
              drawPlayerControlShape(g2, shapeX, stopBounds.y + 2, shapeHeight, "stop");

              if (MouseHandler.inBounds(stopBounds) && MouseHandler.mouseClicked) {
                Replay.controlPlayback("stop");
              }
            }*/
          }
        }

        // Draw software cursor
        /*if (screenshot || Settings.SOFTWARE_CURSOR.get(Settings.currentProfile)) {
          setAlpha(g2, 1.0f);
          g2.drawImage(image_cursor, MouseHandler.x, MouseHandler.y, null);
        }*/

        g2.dispose();

        // Right now is a good time to take a screenshot if one is requested
        /*if (screenshot) {
          try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
            String fname =
                Settings.Dir.SCREENSHOT + "/" + "Screenshot from " + format.format(new Date()) + ".png";
            File screenshotFile = new File(fname);
            ImageIO.write(game_image, "png", screenshotFile);
            if (!quietScreenshot)
              Client.displayMessage(
                  "@cya@Screenshot saved to '" + screenshotFile.toString() + "'", Client.CHAT_NONE);
          } catch (Exception e) {
          }
          screenshot = false;
        }*/

        g.drawImage(game_image, 0, 0, null);

        frames++;

        /*if (Settings.FPS_LIMIT_ENABLED.get(Settings.currentProfile)) {
          int targetFPS = Settings.FPS_LIMIT.get(Settings.currentProfile);

          // pretend that interlacing helps fps while frame limiting
          if (Client.getInterlace()) {
            targetFPS *= 2;
          }
          if (Client.getInterlace() != lastInterlace) {
            if (lastInterlace) {
              sleepTimer *= 2;
              if (sleepTimer > 1250) {
                sleepTimer = 1250;
              }
            } else {
              sleepTimer /= 2;
            }
          }
          lastInterlace = Client.getInterlace();

          // sleep the renderer thread to achieve target FPS
          if (frames > targetFPS / 2 && sleepTimer < 1250) {
            // maximum time to sleep is about 1.25 seconds. Would not like to sleep longer than that
            // ever.
            if (sleepTimer > 500) {
              // we must accelerate the sleep timer incrementation because at sub-3 fps
              // we can't increase it fast enough to reach target of 1 fps
              sleepTimer += 30;
            }
            if (sleepTimer > 300) {
              sleepTimer += 10;
            }
            if (sleepTimer > 200) {
              sleepTimer += 5;
            }
            if (sleepTimer > 100) {
              sleepTimer += 2;
            }
            sleepTimer++;

          } else if (frames < targetFPS / 2 && sleepTimer > 0) {
            if (sleepTimer > 500) {
              // would like to leave this state quickly
              sleepTimer -= 50;
            }
            if (sleepTimer > 300) {
              sleepTimer -= 10;
            }
            if (sleepTimer > 200) {
              sleepTimer -= 5;
            }
            if (sleepTimer > 100) {
              sleepTimer -= 2;
            }
            sleepTimer--;
          }

          try {
            // sleeping the renderer thread lowers the FPS
            Thread.sleep(sleepTimer);
          } catch (Exception e) {
          }
        }

        // calculate FPS
        time = System.currentTimeMillis();
        if (time > fps_timer) {
          fps = frames;
          frames = 0;
          fps_timer = time + 1000;
        }

        // handle resize
        if (width != new_size.width || height != new_size.height) handle_resize();
        if (Settings.fovUpdateRequired) {
          Camera.setFoV(Settings.FOV.get(Settings.currentProfile));
          Settings.fovUpdateRequired = false;
        }*/

        // Reset the mouse click handler
        MouseHandler.mouseClicked = false;
        
        if (width != new_size.width || height != new_size.height) handle_resize();
      }

    public static void drawShadowText(
            Graphics2D g, String text, int x, int y, Color textColor, boolean center) {
        int textX = x;
        int textY = y;
        if (center) {
            Dimension bounds = getStringBounds(g, text);
            textX -= (bounds.width / 2);
            textY += (bounds.height / 2);
        }

        g.setColor(color_shadow);
        g.drawString(text, textX + 1, textY);
        g.drawString(text, textX - 1, textY);
        g.drawString(text, textX, textY + 1);
        g.drawString(text, textX, textY - 1);

        g.setColor(textColor);
        g.drawString(text, textX, textY);
    }

    public static void drawColoredText(Graphics2D g, String text, int x, int y) {
        drawColoredText(g, text, x, y, false);
    }

    public static void drawColoredText(Graphics2D g, String text, int x, int y, boolean center) {
        int textX = x;
        int textY = y;

        if (center) {
            Dimension bounds = getStringBounds(g, text.replaceAll("@...@", ""));
            textX -= (bounds.width / 2);
            textY += (bounds.height / 2);
        }

        String outputText = "";
        Color outputColor = colorFromCode("@yel@");
        Color currentColor = outputColor;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '@' && text.charAt(i + 4) == '@') {
                outputColor = colorFromCode(text.substring(i, i + 4));
                i += 5;
                if (i >= text.length()) break;
            }

            if (currentColor != outputColor) {
                if (outputText.length() > 0) {
                    g.setColor(color_shadow);
                    g.drawString(outputText, textX + 1, textY);
                    g.drawString(outputText, textX - 1, textY);
                    g.drawString(outputText, textX, textY + 1);
                    g.drawString(outputText, textX, textY - 1);

                    g.setColor(currentColor);
                    g.drawString(outputText, textX, textY);
                    textX += getStringBounds(g, outputText).width;
                }
                currentColor = outputColor;
                outputText = "";
            }

            outputText += text.charAt(i);
        }

        g.setColor(color_shadow);
        g.drawString(outputText, textX + 1, textY);
        g.drawString(outputText, textX - 1, textY);
        g.drawString(outputText, textX, textY + 1);
        g.drawString(outputText, textX, textY - 1);

        g.setColor(currentColor);
        g.drawString(outputText, textX, textY);
    }

    public static void drawShadowTextBorder(
            Graphics2D g,
            String text,
            int x,
            int y,
            Color textColor,
            float alpha,
            float boxAlpha,
            boolean border,
            int borderSize) {
        int textX = x;
        int textY = y;
        Dimension bounds = getStringBounds(g, text);
        textX -= (bounds.width / 2);
        textY += (bounds.height / 2);

        g.setColor(color_shadow);
        int rectX = x - (bounds.width / 2) - 2 - borderSize;
        int rectY = y - (bounds.height / 2) + 2 - borderSize;
        int rectWidth = bounds.width + 2 + (borderSize * 2);
        int rectHeight = bounds.height + (borderSize * 2);
        if (border) {
            setAlpha(g, 1.0f);
            g.drawRect(rectX, rectY, rectWidth, rectHeight);
        }
        setAlpha(g, boxAlpha);
        g.fillRect(rectX, rectY, rectWidth, rectHeight);
        setAlpha(g, alpha);
        g.drawString(text, textX + 1, textY);
        g.drawString(text, textX - 1, textY);
        g.drawString(text, textX, textY + 1);
        g.drawString(text, textX, textY - 1);

        g.setColor(textColor);
        g.drawString(text, textX, textY);
    }

    public static void setAlpha(Graphics2D g, float alpha) {
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
    }
    
    public static String getFixedRemoveString(String origString) {
    	// TODO: surfaces' drawstring need to be patched to accommodate >= 1000 offsets
    	int coord = Renderer.width - (512 - 439);
    	return "~" + coord + "~@whi@Remove         WWWWWWWWWW";
    }

    private static Dimension getStringBounds(Graphics2D g, String str) {
        FontRenderContext context = g.getFontRenderContext();
        Rectangle2D bounds = g.getFont().getStringBounds(str, context);
        return new Dimension((int) bounds.getWidth(), (int) bounds.getHeight());
    }


    private static Color colorFromCode(String s) {
        int hexCode = 0xffffff;

        if (s.substring(1, 4).equalsIgnoreCase("red")) hexCode = 0xff0000;
        else if (s.substring(1, 4).equalsIgnoreCase("lre")) hexCode = 0xff9040;
        else if (s.substring(1, 4).equalsIgnoreCase("yel")) hexCode = 0xffff00;
        else if (s.substring(1, 4).equalsIgnoreCase("gre")) hexCode = 65280;
        else if (s.substring(1, 4).equalsIgnoreCase("blu")) hexCode = 255;
        else if (s.substring(1, 4).equalsIgnoreCase("cya")) hexCode = 65535;
        else if (s.substring(1, 4).equalsIgnoreCase("mag")) hexCode = 0xff00ff;
        else if (s.substring(1, 4).equalsIgnoreCase("whi")) hexCode = 0xffffff;
        else if (s.substring(1, 4).equalsIgnoreCase("bla")) hexCode = 0;
        else if (s.substring(1, 4).equalsIgnoreCase("dre")) hexCode = 0xc00000;
        else if (s.substring(1, 4).equalsIgnoreCase("ora")) hexCode = 0xff9040;
        else if (s.substring(1, 4).equalsIgnoreCase("ran")) hexCode = (int) (Math.random() * 16777215D);
        else if (s.substring(1, 4).equalsIgnoreCase("or1")) hexCode = 0xffb000;
        else if (s.substring(1, 4).equalsIgnoreCase("or2")) hexCode = 0xff7000;
        else if (s.substring(1, 4).equalsIgnoreCase("or3")) hexCode = 0xff3000;
        else if (s.substring(1, 4).equalsIgnoreCase("gr1")) hexCode = 0xc0ff00;
        else if (s.substring(1, 4).equalsIgnoreCase("gr2")) hexCode = 0x80ff00;
        else if (s.substring(1, 4).equalsIgnoreCase("gr3")) hexCode = 0x40ff00;

        return new Color(hexCode);
    }

    public static void drawStringCenter(String text, int x, int y, int font, int color) {
        // TODO: find hook, reimplement
        Logger.Info("Unimplemented drawStringCenter called with text: " + text);

        /*
        if (Reflection.drawStringCenter == null) return;


        try {
            Reflection.drawStringCenter.invoke(instance, x, text, color, 0, font, y);
        } catch (Exception e) {
        }
         */
    }


    public static void drawBox(int x, int y, int w, int h, int color) {
        // TODO: find hook, reimplement
        Logger.Debug("Unimplemented drawBox called");

        /*
        if (Reflection.drawBox == null) return;

        try {
            Reflection.drawBox.invoke(instance, x, (byte) -127, color, y, h, w);
        } catch (Exception e) {
        }
         */
    }

    public static void drawBoxBorder(int x, int y, int w, int h, int color) {
        // TODO: find hook, reimplement
        Logger.Debug("Unimplemented drawBoxBorder called");

        /*
        if (Reflection.drawBoxBorder == null) return;

        try {
            Reflection.drawBoxBorder.invoke(instance, x, w, y, 27785, h, color);
        } catch (Exception e) {
        }
         */
    }

}
