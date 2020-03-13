package mrjake.aunis.packet.stargate;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import mrjake.aunis.block.DHDBlock;
import mrjake.aunis.block.stargate.StargateMilkyWayBaseBlock;
import mrjake.aunis.item.AunisItems;
import mrjake.aunis.packet.PositionedPacket;
import mrjake.aunis.stargate.EnumGateState;
import mrjake.aunis.stargate.EnumStargateState;
import mrjake.aunis.stargate.EnumSymbol;
import mrjake.aunis.stargate.StargateNetwork;
import mrjake.aunis.stargate.StargateNetwork.StargatePos;
import mrjake.aunis.stargate.teleportation.TeleportHelper;
import mrjake.aunis.tileentity.DHDTile;
import mrjake.aunis.tileentity.stargate.StargateAbstractBaseTile;
import mrjake.aunis.tileentity.stargate.StargateMilkyWayBaseTile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class StargateRenderingUpdatePacketToServer extends PositionedPacket {
	public StargateRenderingUpdatePacketToServer() {}
	
	private int objectID;
	
	public StargateRenderingUpdatePacketToServer(int objectID, BlockPos pos) {
		super(pos);
		
		this.objectID = objectID;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		super.toBytes(buf);
		
		buf.writeInt(objectID);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		super.fromBytes(buf);
		
		objectID = buf.readInt();
	}

	/**
	 * Closes gate server side
	 * Handles animation packets both sides(initiating and receiving)
	 * Calls closeGate()
	 * 
	 * @param sourceTile - Source StargateBaseTile instance
	 * @param targetOnly When the source gate is broken, don't try to close it.
	 */
	public static void closeGatePacket(StargateAbstractBaseTile sourceTile, boolean targetOnly) {
		if (sourceTile.dialedAddress.size() < 7)
			return;
		
		StargatePos targetGate = StargateNetwork.get(sourceTile.getWorld()).getStargate( sourceTile.dialedAddress );
		World targetWorld = TeleportHelper.getWorld(targetGate.getDimension());
		BlockPos targetPos = targetGate.getPos();
		StargateAbstractBaseTile targetTile = (StargateAbstractBaseTile) targetWorld.getTileEntity(targetPos);
		
		if (!targetOnly) {
			sourceTile.closeGate();
		}

		targetTile.closeGate();
	}
	
	/**
	 * Checks for Stargate at given address and if it's not pointing to itself.
	 * 
	 * @param world Target gate world
	 * @param gateTile Source gate tile
	 * @return True if address vaild
	 */
	public static boolean checkDialedAddress(World world, StargateAbstractBaseTile gateTile) {
		return (StargateNetwork.get(world).stargateInWorld(world, gateTile.dialedAddress) && !gateTile.dialedAddress.subList(0, 6).equals(gateTile.gateAddress.subList(0, 6)));
	}
	
	public static void attemptLightUp(World world, StargateAbstractBaseTile gateTile) {
		if (checkDialedAddress(world, gateTile)) {
			StargateAbstractBaseTile targetTile = StargateNetwork.get(world).getStargate(gateTile.dialedAddress).getTileEntity();
			
			if (targetTile.canAcceptConnectionFrom(gateTile) && gateTile.hasEnergyToDial(targetTile)) {				
				targetTile.incomingWormhole(gateTile.gateAddress, gateTile.dialedAddress.size());
			}
		 }
	}
	
	public static EnumGateState attemptOpen(World world, StargateAbstractBaseTile gateTile, @Nullable DHDTile sourceDhdTile, boolean stopRing) {
		EnumGateState gateState = EnumGateState.OK;
		
		// Check if symbols entered match the range, last is ORIGIN, target gate exists, and if not dialing self
		if (checkDialedAddress(world, gateTile)) {
			StargateAbstractBaseTile targetTile = StargateNetwork.get(world).getStargate(gateTile.dialedAddress).getTileEntity();

			if (targetTile.canAcceptConnectionFrom(gateTile)) {			
				
				if (gateTile.hasEnergyToDial(targetTile)) {
					
					if (sourceDhdTile != null) 
						sourceDhdTile.activateSymbol(EnumSymbol.BRB);
									
					gateTile.openGate(true, null, false);
					
					boolean eightChevronDial = gateTile.dialedAddress.size() == 8;
					targetTile.openGate(false, gateTile.gateAddress, eightChevronDial);
					
					if (targetTile instanceof StargateMilkyWayBaseTile) {
						if (((StargateMilkyWayBaseTile) targetTile).isLinked()) 
							((StargateMilkyWayBaseTile) targetTile).getLinkedDHD(targetTile.getWorld()).activateSymbol(EnumSymbol.BRB);
					}
				}
			
				else {
					gateState = EnumGateState.NOT_ENOUGH_POWER;
				}
			}
			
			else {
				// Target gate busy or cannot accept connection
				// Return address malformed
				
				gateState = EnumGateState.ADDRESS_MALFORMED;
			}
		}
		
		else {
			// Address malformed, dialing failed
			// Execute GATE_DIAL_FAILED
			
			gateState = EnumGateState.ADDRESS_MALFORMED;
		}
		
		if (!gateState.ok()) {			
			gateTile.dialingFailed();
		}
		
		return gateState;
	}
	
	
	public static class GateRenderingUpdatePacketToServerHandler implements IMessageHandler<StargateRenderingUpdatePacketToServer, IMessage> {		
		@Override
		public IMessage onMessage(StargateRenderingUpdatePacketToServer message, MessageContext ctx) {
			ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
			
				EntityPlayerMP player = ctx.getServerHandler().player;
				World world = player.getEntityWorld();
				BlockPos pos = message.pos;
				
				Block block = world.getBlockState(pos).getBlock();
				
				if ( block instanceof StargateMilkyWayBaseBlock || block instanceof DHDBlock ) {					
					TileEntity te = world.getTileEntity(pos);
					StargateMilkyWayBaseTile gateTile;
					DHDTile dhdTile;
					
					if ( te instanceof StargateMilkyWayBaseTile ) {
						gateTile = (StargateMilkyWayBaseTile) te;
						dhdTile = gateTile.getLinkedDHD(world);
					}
					
					else if ( te instanceof DHDTile ) {
						dhdTile = (DHDTile) te;
						gateTile = (StargateMilkyWayBaseTile) dhdTile.getLinkedGate(world);
					}
					
					else {
						// Bad BlockPos given
						
						return;
					}
					
					if (dhdTile != null && gateTile != null) {	
						if (gateTile.getStargateState().idle() || gateTile.getStargateState().engaged()) {
							EnumSymbol symbol = EnumSymbol.valueOf(message.objectID);
							
							/*
							 * Check power requirements
							 * At least some power is required to begin dialing
							 * If no power or no crystal, DHD will appear dead
							 */
							
//							ItemStackHandler itemStackHandler = (ItemStackHandler) dhdTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
//							ItemStack powerCrystal = itemStackHandler.getStackInSlot(0);
							
//							if (powerCrystal.isEmpty()) {
//								// No control crystal, display message
//								player.sendStatusMessage(new TextComponentTranslation("tile.aunis.dhd_block.no_crystal_warn"), true);
//								
//								return;
//							}
							
							if ( symbol == EnumSymbol.BRB ) {						
								if ( gateTile.getStargateState().engaged() ) {
									if ( gateTile.getStargateState().initiating() ) {									
										closeGatePacket(gateTile, false);
									}
									
									else {
										player.sendStatusMessage(new TextComponentTranslation("tile.aunis.dhd_block.incoming_wormhole_warn"), true);
									}
								}
								
								else {
									// Gate is closed, BRB pressed
									
									if (gateTile.getStargateState() == EnumStargateState.UNSTABLE)
										return;
									
									ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
									if (stack.getItem() == AunisItems.dialerFast) {
										NBTTagCompound compound = stack.getTagCompound();
										
										byte[] symbols = compound.getByteArray("address");
										List<EnumSymbol> dialedAddress = new ArrayList<EnumSymbol>();
										
										for (byte s : symbols)
											dialedAddress.add(EnumSymbol.valueOf(s));
										
										dialedAddress.add(EnumSymbol.ORIGIN);
										
										gateTile.dialedAddress = dialedAddress;
									}
									
									// Check if symbols entered match the range, last is ORIGIN, target gate exists, and if not dialing self
									EnumGateState gateState = attemptOpen(world, gateTile, dhdTile, true);
									
									if (gateState != EnumGateState.OK) {
//										((StargateMilkyWayBaseTile) gateTile).setRollPlayed();
										
										if (gateState == EnumGateState.NOT_ENOUGH_POWER)
											player.sendStatusMessage(new TextComponentTranslation("tile.aunis.stargatebase_block.not_enough_power"), true);
									}
								}	
								
							}
							
							// Not BRB, some glyph pressed
							else {								
								if ( gateTile.canAddSymbol(symbol) ) {
									gateTile.addSymbolToAddressDHD(symbol);
									
									// We can still add glyphs(no limit reached)
									int symbolCount = gateTile.getEnteredSymbolsCount();				
																	
									// Update the DHD's renderer
//									dhdTile.activateSymbol(message.objectID);
															
									boolean lock = symbol == EnumSymbol.ORIGIN;
									
									gateTile.sendSignal(null, "stargate_dhd_chevron_engaged", new Object[] { symbolCount, lock, symbol.englishName });
									
									StargateNetwork network = StargateNetwork.get(world);
									
									if (gateTile.dialedAddress.equals(StargateNetwork.EARTH_ADDRESS) && network.hasLastActivatedOrlinAddress()) {
										gateTile.dialedAddress.clear();
										gateTile.dialedAddress.addAll(StargateNetwork.get(world).getLastActivatedOrlinAddress());
										gateTile.dialedAddress.add(EnumSymbol.ORIGIN);
									}
									
									// Light up target gate, if exists
									if (lock) {	
										attemptLightUp(world, gateTile);
									}
								} // add symbol if
							} // not brb else
						} // Not busy if
						
//						else { 
//							switch (gateTile.getStargateState()) {
//								case COMPUTER_DIALING:
//									player.sendStatusMessage(new TextComponentTranslation("tile.aunis.dhd_block.computer_dial"), true);
//									break;
//									
////								case FAILING:
////									player.sendStatusMessage(new TextComponentString(Aunis.proxy.localize("tile.aunis.dhd_block.failing_dial")), true);
////									break;
//									
//								default:
//									break;
//							}
//						}
					} // gateTile and dhdTile not null if
					
					else {
						// DHD is not linked
						player.sendStatusMessage(new TextComponentTranslation("tile.aunis.dhd_block.not_linked_warn"), true);
					}
					
				} // block loaded if
			}); // runnable

			return null;
		} // IMessage onMessage end
	} // Handler end
}
