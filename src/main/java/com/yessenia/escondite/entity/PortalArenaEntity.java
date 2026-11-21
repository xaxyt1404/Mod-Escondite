package com.yessenia.escondite.entity;

import com.yessenia.escondite.game.GameManager;
import com.yessenia.escondite.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PortalArenaEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_ARENA_ID =
            SynchedEntityData.defineId(PortalArenaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_NOMBRE =
            SynchedEntityData.defineId(PortalArenaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_BLOQUE =
            SynchedEntityData.defineId(PortalArenaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ESTADO =
            SynchedEntityData.defineId(PortalArenaEntity.class, EntityDataSerializers.STRING);

    public int tiempoCliente = 0;

    public PortalArenaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.tiempoCliente = this.random.nextInt(10000);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ARENA_ID, "");
        builder.define(DATA_NOMBRE, "Arena");
        builder.define(DATA_BLOQUE, "minecraft:stone");
        builder.define(DATA_ESTADO, "§aDisponible");
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tiempoCliente++;
        }
    }

    @Override
    public boolean isPickable() {
        return true; // Permite recibir clic izquierdo y clic derecho
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            procesarEntrada(sp);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && source.getEntity() instanceof ServerPlayer sp) {
            procesarEntrada(sp);
        }
        return false; // Nunca recibe daño ni se destruye
    }

    private void procesarEntrada(ServerPlayer player) {
        String id = getArenaId();
        if (id == null || id.isEmpty()) return;

        player.serverLevel().playSound(null, this.blockPosition(), ModSounds.PORTAL_ENTRAR, SoundSource.PLAYERS, 0.9f, 1.15f);
        GameManager.entrarArena(player, id, player.getServer());
    }

    // ==================== GETTERS Y SETTERS SINCRONIZADOS ====================

    public void setDatosPortal(String arenaId, String nombre, String bloqueBase, String lineaEstado) {
        this.entityData.set(DATA_ARENA_ID, arenaId != null ? arenaId : "");
        this.entityData.set(DATA_NOMBRE, nombre != null ? nombre : "Arena");
        this.entityData.set(DATA_BLOQUE, bloqueBase != null ? bloqueBase : "minecraft:stone");
        this.entityData.set(DATA_ESTADO, lineaEstado != null ? lineaEstado : "§aDisponible");
    }

    public String getArenaId() {
        return this.entityData.get(DATA_ARENA_ID);
    }

    public String getNombreArena() {
        return this.entityData.get(DATA_NOMBRE);
    }

    public String getBloqueBase() {
        return this.entityData.get(DATA_BLOQUE);
    }

    public String getLineaEstado() {
        return this.entityData.get(DATA_ESTADO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("ArenaId")) {
            this.entityData.set(DATA_ARENA_ID, tag.getString("ArenaId"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("ArenaId", getArenaId());
    }
}