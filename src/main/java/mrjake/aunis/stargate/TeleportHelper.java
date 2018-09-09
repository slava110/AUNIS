package mrjake.aunis.stargate;

import javax.vecmath.Vector2f;

import org.lwjgl.util.vector.Matrix2f;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class TeleportHelper {
	
	private static void translateTo00(Vector2f center, Vector2f v) {
		v.x -= center.x;
		v.y -= center.y;
	}
	
	public static void rotateAround00(Vector2f v, float rotation, String flip) {
		Matrix2f m = new Matrix2f();
		Matrix2f p = new Matrix2f();
		
		float sin = MathHelper.sin(rotation);
		float cos = MathHelper.cos(rotation);
		
		if (flip != null) {
			if ( flip.equals("x") )
				v.x *= -1;
			else
				v.y *= -1;
		}
		
		m.m00 = cos;	m.m10 = -sin;
		m.m01 = sin;	m.m11 =  cos;
		p.m00 = v.x;	p.m10 = 0;
		p.m01 = v.y;	p.m11 = 0;
		
		Matrix2f out = Matrix2f.mul(m, p, null);
		
		v.x = out.m00;
		v.y = out.m01;
	}
	
	private static void translateToDest(Vector2f v, Vector2f dest) {
		v.x += dest.x;
		v.y += dest.y;
	}
	
	public static World getWorld(int dimension) {
		World world = DimensionManager.getWorld(0);
		
		if (dimension == 0)
			return world;
		
		return world.getMinecraftServer().getWorld(dimension);
	}
	
	public static void teleportServer(Entity player, BlockPos sourceGatePos, BlockPos targetGatePos, int rotationAngle, String sourceAxisName, Vector2f motionVector, int targetDimension, float dimensionMul) {
		int sourceDimension = player.getEntityWorld().provider.getDimension();
		
		Vec3d lookVec = player.getLookVec();
		Vec3d playerPos = player.getPositionVector();
		
		/*if (sourceDimension != targetDimension)
			player.changeDimension(targetDimension);*/
		
		//player.getServer().getPlayerList().transferEntityToWorld(player, sourceDimension, (WorldServer)player.getEntityWorld(), (WorldServer)TeleportHelper.getWorld(targetDimension));
		player.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) player, targetDimension, new Teleporter((WorldServer) player.getEntityWorld()));
		/* teleporter.playerNetServerHandler.setPlayerLocation
		 			(posX, posY, posZ, teleporter.rotationYaw, teleporter.rotationPitch);*/
		float rotation = (float) Math.toRadians(rotationAngle);
		
		setRotation(player, lookVec, rotation);
		teleport(player, playerPos, sourceGatePos, targetGatePos, rotation, sourceAxisName);
		setMotion(player, rotation, motionVector);
	}
	
	public static void setRotation(Entity player, Vec3d lookVec, float rotation) {
		Vector2f lookVec2f = new Vector2f( (float)(lookVec.x), (float)(lookVec.z) );
		
		rotateAround00(lookVec2f, rotation, null);
		
		float targetYaw = (float) Math.toDegrees(MathHelper.atan2(-lookVec2f.x, lookVec2f.y));
		
		player.rotationYaw = targetYaw;
	}
	
	public static void setMotion(Entity player, float rotation, Vector2f motionVec2f) {		
		if (motionVec2f != null) {		
			rotateAround00(motionVec2f, rotation, null);
					
			player.motionX = motionVec2f.x;
			player.motionZ = motionVec2f.y;
			player.velocityChanged = true;
		}
	}
	
	public static boolean frontSide(EnumFacing sourceFacing, Vector2f motionVec) {
		Axis axis = sourceFacing.getAxis();
		AxisDirection direction = sourceFacing.getAxisDirection();
		float motion;
		
		if (axis == Axis.X)
			motion = motionVec.x;			
		else
			motion = motionVec.y;
				
		// If facing positive, then player should move negative
		if (direction == AxisDirection.POSITIVE)
			return motion <= 0;
		else
			return motion >= 0;
	}
	
	public static void teleport(Entity player, Vec3d playerPos, BlockPos sourceGatePos, BlockPos targetGatePos, float rotation, String sourceAxisName) {
		Vector2f sourceCenter = new Vector2f( sourceGatePos.getX()+0.5f, sourceGatePos.getZ()+0.5f );
		Vector2f destCenter = new Vector2f( targetGatePos.getX()+0.5f, targetGatePos.getZ()+0.5f );
		Vector2f playerPosition = new Vector2f( (float)playerPos.x, (float)playerPos.z );  
		
		translateTo00(sourceCenter, playerPosition);
		rotateAround00(playerPosition, rotation, sourceAxisName);				
		translateToDest(playerPosition, destCenter);
		
		float y = (float) (targetGatePos.getY() + ( playerPos.y - sourceGatePos.getY() ));
		player.setPositionAndUpdate(playerPosition.x, y, playerPosition.y);
	}
	
}