package com.owlmaddie.chat;

import static com.owlmaddie.particle.Particles.FIRE_BIG_PARTICLE;
import static com.owlmaddie.particle.Particles.FIRE_SMALL_PARTICLE;
import static com.owlmaddie.particle.Particles.FLEE_PARTICLE;
import static com.owlmaddie.particle.Particles.FOLLOW_ENEMY_PARTICLE;
import static com.owlmaddie.particle.Particles.FOLLOW_FRIEND_PARTICLE;
import static com.owlmaddie.particle.Particles.HEART_BIG_PARTICLE;
import static com.owlmaddie.particle.Particles.HEART_SMALL_PARTICLE;
import static com.owlmaddie.particle.Particles.LEAD_ENEMY_PARTICLE;
import static com.owlmaddie.particle.Particles.LEAD_FRIEND_PARTICLE;
import static com.owlmaddie.network.ServerPackets.LOGGER;
import static com.owlmaddie.particle.Particles.PROTECT_PARTICLE;

import java.util.List;
import java.util.UUID;

import com.owlmaddie.controls.SpeedControls;
import com.owlmaddie.goals.AttackPlayerGoal;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.FleePlayerGoal;
import com.owlmaddie.goals.FollowPlayerGoal;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.LeadPlayerGoal;
import com.owlmaddie.goals.ProtectPlayerGoal;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.message.Behavior;
import com.owlmaddie.particle.ParticleEmitter;
import com.owlmaddie.utils.ServerEntityFinder;
import com.owlmaddie.utils.VillagerEntityAccessor;
import com.owlmaddie.utils.WitherEntityAccessor;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.VillageGossipType;
import net.minecraft.world.GameRules;

public class BehaviorApplier {
    public static void apply(List<Behavior> behaviors, ServerPlayerEntity player, UUID entityId, PlayerData playerData) {
        MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(player.getServerWorld(),
                entityId);
        // Determine entity's default speed
        // Some Entities (i.e. Axolotl) set this incorrectly... so adjusting in the
        // SpeedControls class
        float entitySpeed = SpeedControls.getMaxSpeed(entity);
        float entitySpeedMedium = MathHelper.clamp(entitySpeed * 1.15F, 0.5f, 1.15f);
        float entitySpeedFast = MathHelper.clamp(entitySpeed * 1.3F, 0.5f, 1.3f);
        

        for (Behavior behavior : behaviors) {
            LOGGER.info("Behavior: " + behavior.getName()
                    + (behavior.getArgument() != null ? ", Argument: " + behavior.getArgument() : ""));

            // Apply behaviors to entity
            if (behavior.getName().equals("FOLLOW")) {
                FollowPlayerGoal followGoal = new FollowPlayerGoal(player, entity, entitySpeedMedium);
                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                EntityBehaviorManager.addGoal(entity, followGoal, GoalPriority.FOLLOW_PLAYER);
                if (playerData.friendship >= 0) {
                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity,
                            FOLLOW_FRIEND_PARTICLE, 0.5, 1);
                } else {
                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FOLLOW_ENEMY_PARTICLE,
                            0.5, 1);
                }

            } else if (behavior.getName().equals("UNFOLLOW")) {
                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);

            } else if (behavior.getName().equals("FLEE")) {
                float fleeDistance = 40F;
                FleePlayerGoal fleeGoal = new FleePlayerGoal(player, entity, entitySpeedFast, fleeDistance);
                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                EntityBehaviorManager.addGoal(entity, fleeGoal, GoalPriority.FLEE_PLAYER);
                ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FLEE_PARTICLE, 0.5, 1);

            } else if (behavior.getName().equals("UNFLEE")) {
                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);

            } else if (behavior.getName().equals("ATTACK")) {
                AttackPlayerGoal attackGoal = new AttackPlayerGoal(player, entity, entitySpeedFast);
                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                EntityBehaviorManager.addGoal(entity, attackGoal, GoalPriority.ATTACK_PLAYER);
                ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FLEE_PARTICLE, 0.5, 1);

            } else if (behavior.getName().equals("PROTECT")) {
                if (playerData.friendship <= 0) {
                    // force friendship to prevent entity from attacking player when protecting
                    playerData.friendship = 1;
                }
                ProtectPlayerGoal protectGoal = new ProtectPlayerGoal(player, entity, 1.0);
                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                EntityBehaviorManager.addGoal(entity, protectGoal, GoalPriority.PROTECT_PLAYER);
                ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, PROTECT_PARTICLE, 0.5, 1);

            } else if (behavior.getName().equals("UNPROTECT")) {
                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);

            } else if (behavior.getName().equals("LEAD")) {
                LeadPlayerGoal leadGoal = new LeadPlayerGoal(player, entity, entitySpeedMedium);
                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                EntityBehaviorManager.addGoal(entity, leadGoal, GoalPriority.LEAD_PLAYER);
                if (playerData.friendship >= 0) {
                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, LEAD_FRIEND_PARTICLE,
                            0.5, 1);
                } else {
                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, LEAD_ENEMY_PARTICLE,
                            0.5, 1);
                }
            } else if (behavior.getName().equals("UNLEAD")) {
                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);

            } else if (behavior.getName().equals("FRIENDSHIP")) {
                int new_friendship = Math.max(-3, Math.min(3, behavior.getArgument()));

                // Does friendship improve?
                if (new_friendship > playerData.friendship) {
                    // Stop any attack/flee if friendship improves
                    EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                    EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);

                    if (entity instanceof WitherEntity && new_friendship == 3) {
                        // Best friend a Nether and get a NETHER_STAR
                        WitherEntity wither = (WitherEntity) entity;
                        ((WitherEntityAccessor) wither)
                                .callDropEquipment(((ServerWorld) entity.getWorld()),entity.getWorld().getDamageSources().generic(),true);
                        entity.getWorld().playSound(entity, entity.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH,
                                SoundCategory.PLAYERS, 0.3F, 1.0F);
                    }

                    if (entity instanceof EnderDragonEntity && new_friendship == 3) {
                        // Trigger end of game (friendship always wins!)
                        EnderDragonEntity dragon = (EnderDragonEntity) entity;

                        // Emit particles & sound
                        ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity,
                                HEART_BIG_PARTICLE, 3, 200);
                        entity.getWorld().playSound(entity, entity.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                                SoundCategory.PLAYERS, 0.3F, 1.0F);
                        entity.getWorld().playSound(entity, entity.getBlockPos(),
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.5F, 1.0F);

                        // Check if the game rule for mob loot is enabled
                        boolean doMobLoot = entity.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT);

                        // If this is the first time the dragon is 'befriended', adjust the XP
                        int baseXP = 500;
                        if (dragon.getFight() != null && !dragon.getFight().hasPreviouslyKilled()) {
                            baseXP = 12000;
                        }

                        // If the world is a server world and mob loot is enabled, spawn XP orbs
                        if (entity.getWorld() instanceof ServerWorld && doMobLoot) {
                            // Loop to spawn XP orbs
                            for (int j = 1; j <= 11; j++) {
                                float xpFraction = (j == 11) ? 0.2F : 0.08F;
                                int xpAmount = MathHelper.floor((float) baseXP * xpFraction);
                                ExperienceOrbEntity.spawn((ServerWorld) entity.getWorld(), entity.getPos(), xpAmount);
                            }
                        }

                        // Mark fight as over
                        dragon.getFight().dragonKilled(dragon);
                    }
                }

                // Merchant deals (if friendship changes with a Villager
                if (entity instanceof VillagerEntity && playerData.friendship != new_friendship) {
                    VillagerEntityAccessor villager = (VillagerEntityAccessor) entity;
                    switch (new_friendship) {
                        case 3:
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MAJOR_POSITIVE, 20);
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 25);
                            break;
                        case 2:
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 25);
                            break;
                        case 1:
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 10);
                            break;
                        case -1:
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 10);
                            break;
                        case -2:
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 25);
                            break;
                        case -3:
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MAJOR_NEGATIVE, 20);
                            villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 25);
                            break;
                    }
                }

                // Tame best friends and un-tame worst enemies
                if (entity instanceof TameableEntity && playerData.friendship != new_friendship) {
                    TameableEntity tamableEntity = (TameableEntity) entity;
                    if (new_friendship == 3 && !tamableEntity.isTamed()) {
                        tamableEntity.setOwner(player);
                    } else if (new_friendship == -3 && tamableEntity.isTamed()) {
                        tamableEntity.setTamed(false,true);
                        tamableEntity.setOwnerUuid(null);
                    }
                }

                // Emit friendship particles
                if (playerData.friendship != new_friendship) {
                    int friendDiff = new_friendship - playerData.friendship;
                    if (friendDiff > 0) {
                        // Heart particles
                        if (new_friendship == 3) {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity,
                                    HEART_BIG_PARTICLE, 0.5, 10);
                        } else {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity,
                                    HEART_SMALL_PARTICLE, 0.1, 1);
                        }

                    } else if (friendDiff < 0) {
                        // Fire particles
                        if (new_friendship == -3) {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity,
                                    FIRE_BIG_PARTICLE, 0.5, 10);
                        } else {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity,
                                    FIRE_SMALL_PARTICLE, 0.1, 1);
                        }
                    }
                }

                playerData.friendship = new_friendship;
            }
        }

    }
}
