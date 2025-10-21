package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.*;
// import static com.owlmaddie.network.ServerPackets.FOLLOW_ENEMY_PARTICLE;
// import static com.owlmaddie.network.ServerPackets.LOGGER;

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
import com.owlmaddie.utils.TameableHelper;
import com.owlmaddie.utils.VillagerEntityAccessor;
import com.owlmaddie.utils.WitherEntityAccessor;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameRules;

public class BehaviorApplier {
    public static void apply(List<Behavior> behaviors, ServerPlayer player, String entityId, PlayerData playerData ){
        Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), UUID.fromString(entityId));
        if(entity == null){
            return;
        }
        float entitySpeed = SpeedControls.getMaxSpeed(entity);
        float entitySpeedMedium = Mth.clamp(entitySpeed * 1.15F, 0.5f, 1.15f);
        float entitySpeedFast = Mth.clamp(entitySpeed * 1.3F, 0.5f, 1.3f);


        for (Behavior behavior : behaviors) {
                        LOGGER.info("Behavior: " + behavior.getName() + (behavior.getArgument() != null ?
                                ", Argument: " + behavior.getArgument() : ""));

                        // Apply behaviors to entity
                        if (behavior.getName().equals("FOLLOW")) {
                            FollowPlayerGoal followGoal = new FollowPlayerGoal(player, entity, entitySpeedMedium);
                            EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                            EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                            EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                            EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                            EntityBehaviorManager.addGoal(entity, followGoal, GoalPriority.FOLLOW_PLAYER);
                            if (playerData.friendship >= 0) {
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FOLLOW_ENEMY_PARTICLE, 0.5, 1);
                            } else {
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FOLLOW_ENEMY_PARTICLE, 0.5, 1);
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
                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FLEE_PARTICLE, 0.5, 1);

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
                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FLEE_PARTICLE, 0.5, 1);
                        } else if (behavior.getName().equals("UNATTACK")) {
                            EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                            AttackPlayerGoal.stopAttack(entity);
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
                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) PROTECT_PARTICLE, 0.5, 1);

                        } else if (behavior.getName().equals("UNPROTECT")) {
                            EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);

                        } else if (behavior.getName().equals("LEAD")) {
                            LeadPlayerGoal leadGoal = new LeadPlayerGoal(player, entity, entitySpeedMedium);
                            EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                            EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                            EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                            EntityBehaviorManager.addGoal(entity, leadGoal, GoalPriority.LEAD_PLAYER);
                            if (playerData.friendship >= 0) {
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) LEAD_FRIEND_PARTICLE, 0.5, 1);
                            } else {
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) LEAD_ENEMY_PARTICLE, 0.5, 1);
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

                                if (entity instanceof WitherBoss && new_friendship == 3) {
                                    // Best friend a Nether and get a NETHER_STAR
                                    WitherBoss wither = (WitherBoss) entity;
                                    ((WitherEntityAccessor) wither).callDropEquipment(entity.level().damageSources().generic(), 1, true);
                                    entity.level().playSound(entity, entity.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.3F, 1.0F);
                                }

                                if (entity instanceof EnderDragon && new_friendship == 3) {
                                    // Trigger end of game (friendship always wins!)
                                    EnderDragon dragon = (EnderDragon) entity;

                                    // Emit particles & sound
                                    ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) HEART_BIG_PARTICLE, 3, 200);
                                    entity.level().playSound(entity, entity.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 0.3F, 1.0F);
                                    entity.level().playSound(entity, entity.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 1.0F);

                                    // Check if the game rule for mob loot is enabled
                                    ServerLevel serverWorld = (ServerLevel) entity.level();
                                    boolean doMobLoot = serverWorld.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);

                                    // If this is the first time the dragon is 'befriended', adjust the XP
                                    int baseXP = 500;
                                    if (dragon.getDragonFight() != null && !dragon.getDragonFight().hasPreviouslyKilledDragon()) {
                                        baseXP = 12000;
                                    }

                                    // If the world is a server world and mob loot is enabled, spawn XP orbs
                                    if (entity.level() instanceof ServerLevel && doMobLoot) {
                                        // Loop to spawn XP orbs
                                        for (int j = 1; j <= 11; j++) {
                                            float xpFraction = (j == 11) ? 0.2F : 0.08F;
                                            int xpAmount = Mth.floor((float) baseXP * xpFraction);
                                            ExperienceOrb.award((ServerLevel) entity.level(), entity.position(), xpAmount);
                                        }
                                    }

                                    // Mark fight as over
                                    dragon.getDragonFight().setDragonKilled(dragon);
                                }
                            }

                            // Merchant deals (if friendship changes with a Villager
                            if (entity instanceof Villager && playerData.friendship != new_friendship) {
                                VillagerEntityAccessor villager = (VillagerEntityAccessor) entity;
                                switch (new_friendship) {
                                    case 3:
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MAJOR_POSITIVE, 20);
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MINOR_POSITIVE, 25);
                                        break;
                                    case 2:
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MINOR_POSITIVE, 25);
                                        break;
                                    case 1:
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MINOR_POSITIVE, 10);
                                        break;
                                    case -1:
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MINOR_NEGATIVE, 10);
                                        break;
                                    case -2:
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MINOR_NEGATIVE, 25);
                                        break;
                                    case -3:
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MAJOR_NEGATIVE, 20);
                                        GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                GossipTypeHelper.MINOR_NEGATIVE, 25);
                                        break;
                                }
                            }


                            // Tame best friends and un-tame worst enemies
                            if (entity instanceof TamableAnimal && playerData.friendship != new_friendship) {
                                TamableAnimal tamableEntity = (TamableAnimal) entity;
                                if (new_friendship == 3 && !tamableEntity.isTame()) {
                                    tamableEntity.tame(player);
                                } else if (new_friendship == -3 && tamableEntity.isTame()) {
                                    TameableHelper.setTamed((TamableAnimal) entity, false);
                                    TameableHelper.clearOwner(tamableEntity);
                                }
                            }

                            // Emit friendship particles
                            if (playerData.friendship != new_friendship) {
                                int friendDiff = new_friendship - playerData.friendship;
                                if (friendDiff > 0) {
                                    // Heart particles
                                    if (new_friendship == 3) {
                                        ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) HEART_BIG_PARTICLE, 0.5, 10);
                                    } else {
                                        ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) HEART_SMALL_PARTICLE, 0.1, 1);
                                    }

                                } else if (friendDiff < 0) {
                                    // Fire particles
                                    if (new_friendship == -3) {
                                        ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FIRE_BIG_PARTICLE, 0.5, 10);
                                    } else {
                                        ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FIRE_SMALL_PARTICLE, 0.1, 1);
                                    }
                                }
                            }

                            playerData.friendship = new_friendship;
                        }
                    }
    }
}
