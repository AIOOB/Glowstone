package net.glowstone.entity.objects;

import com.flowpowered.network.Message;
import com.google.common.base.Preconditions;
import net.glowstone.entity.GlowEntity;
import net.glowstone.net.message.play.entity.DestroyEntitiesMessage;
import net.glowstone.net.message.play.entity.SpawnXpOrbMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GlowExperienceOrb extends GlowEntity implements ExperienceOrb {

    private static final int LIFETIME = 5 * 60 * 20;

    private boolean fromBottle;
    private int experience;
    private boolean tickSkipped = false;

    public GlowExperienceOrb(Location location) {
        this(location, 1);
    }

    public GlowExperienceOrb(Location location, int experience) {
        super(location);
        setBoundingBox(0.5, 0.5);
        this.experience = experience;
        this.fromBottle = false;
    }

    @Override
    public List<Message> createSpawnMessage() {
        Location location = getLocation();
        return Collections.singletonList(new SpawnXpOrbMessage(getEntityId(), location.getX(), location.getY(), location.getZ(), (short) getExperience()));
    }

    @Override
    public void damage(double amount, Entity source, EntityDamageEvent.DamageCause cause) {
        if (!isInvulnerable()) {
            remove();
        }
    }

    @Override
    public void pulse() {
        super.pulse();
        if (tickSkipped) {
            // find player to give experience
            // todo: drag self towards player
            Optional<Player> player = getWorld().getPlayers().stream()
                    .filter(p -> p.getLocation().distanceSquared(location) <= 1)
                    .findAny();
            if (player.isPresent()) {
                player.get().giveExp(experience);
                world.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                remove();
                return;
            }
        }
        if (getTicksLived() > LIFETIME) {
            remove();
            return;
        }
        if (!tickSkipped) {
            tickSkipped = true;
        }
    }

    private void refresh() {
        DestroyEntitiesMessage destroyMessage = new DestroyEntitiesMessage(Collections.singletonList(this.getEntityId()));
        List<Message> spawnMessages = this.createSpawnMessage();
        Message[] messages = new Message[]{destroyMessage, spawnMessages.get(0)};
        getWorld()
                .getRawPlayers()
                .stream()
                .filter(p -> p.canSeeEntity(this))
                .forEach(p -> p.getSession().sendAll(messages));
    }

    @Override
    public int getExperience() {
        return experience;
    }

    @Override
    public void setExperience(int experience) {
        Preconditions.checkArgument(experience > 0, "Experience points cannot be negative.");
        this.experience = experience;
        refresh();
    }

    public void setFromBottle(boolean fromBottle) {
        this.fromBottle = fromBottle;
    }

    @Override
    public boolean isFromBottle() {
        return fromBottle;
    }

    @Override
    public EntityType getType() {
        return EntityType.EXPERIENCE_ORB;
    }
}
