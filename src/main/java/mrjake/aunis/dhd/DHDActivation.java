package mrjake.aunis.dhd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import mrjake.aunis.block.BlockRotated;
import mrjake.aunis.packet.AunisPacketHandler;
import mrjake.aunis.packet.gate.renderingUpdate.GateRenderingUpdatePacketToServer;
import mrjake.aunis.packet.gate.renderingUpdate.GateRenderingUpdatePacket.EnumPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DHDActivation {
	private static List<Vector3f> dhdVertices = Arrays.asList(
			new Vector3f( 0.194732f, 0.41862f, 0.734536f ),
			new Vector3f( 0.131432f, 0.314995f, 0.884427f ),
			new Vector3f( 0.071232f, 0.21041f, 1.02261f ),
			new Vector3f( 0.351322f, 0.339657f, 0.781297f ),
			new Vector3f( 0.237242f, 0.261759f, 0.915953f ),
			new Vector3f( 0.128182f, 0.181499f, 1.039731f ),
			new Vector3f( 0.469472f, 0.221105f, 0.851502f ),
			new Vector3f( 0.317212f, 0.18185f, 0.963274f ),
			new Vector3f( 0.171142f, 0.138253f, 1.065341f ),
			new Vector3f( 0.536292f, 0.075897f, 0.937493f ),
			new Vector3f( 0.362702f, 0.083923f, 1.021265f ),
			new Vector3f( 0.195462f, 0.085344f, 1.096672f ),
			new Vector3f( 0.544482f, -0.08011f, 1.029879f ),
			new Vector3f( 0.368752f, -0.021404f, 1.083639f ),
			new Vector3f( 0.198502f, 0.028512f, 1.130328f ),
			new Vector3f( 0.493172f, -0.229914f, 1.11859f ),
			new Vector3f( 0.334732f, -0.122722f, 1.143638f ),
			new Vector3f( 0.179942f, -0.026092f, 1.162664f ),
			new Vector3f( 0.387932f, -0.357156f, 1.193942f ),
			new Vector3f( 0.264312f, -0.209033f, 1.19475f ),
			new Vector3f( 0.141792f, -0.072556f, 1.190179f ),
			new Vector3f( 0.222882f, -0.454946f, 1.251851f ),
			new Vector3f( 0.153402f, -0.275803f, 1.234291f ),
			new Vector3f( 0.081842f, -0.108436f, 1.211427f ),
			new Vector3f( 0.047192f, -0.494148f, 1.275066f ),
			new Vector3f( 0.035032f, -0.303148f, 1.250484f ),
			new Vector3f( 0.017932f, -0.123047f, 1.220079f ),
			new Vector3f( -0.133748f, -0.481964f, 1.267851f ),
			new Vector3f( -0.087238f, -0.295938f, 1.246214f ),
			new Vector3f( -0.048018f, -0.119011f, 1.217689f ),
			new Vector3f( -0.300218f, -0.419719f, 1.230991f ),
			new Vector3f( -0.200168f, -0.254955f, 1.221944f ),
			new Vector3f( -0.108888f, -0.096777f, 1.204523f ),
			new Vector3f( -0.421968f, -0.327041f, 1.176108f ),
			new Vector3f( -0.291498f, -0.184631f, 1.180299f ),
			new Vector3f( -0.158068f, -0.058746f, 1.182001f ),
			new Vector3f( -0.514048f, -0.192459f, 1.09641f ),
			new Vector3f( -0.351348f, -0.092598f, 1.125798f ),
			new Vector3f( -0.190238f, -0.009046f, 1.15257f ),
			new Vector3f( -0.550158f, -0.039411f, 1.005777f ),
			new Vector3f( -0.373208f, 0.011163f, 1.064352f ),
			new Vector3f( -0.201908f, 0.04696f, 1.119403f ),
			new Vector3f( -0.526348f, 0.115408f, 0.914095f ),
			new Vector3f( -0.354728f, 0.115415f, 1.002616f ),
			new Vector3f( -0.191838f, 0.103181f, 1.08611f ),
			new Vector3f( -0.445228f, 0.25511f, 0.831365f ),
			new Vector3f( -0.297898f, 0.20885f, 0.947285f ),
			new Vector3f( -0.161088f, 0.153552f, 1.056281f ),
			new Vector3f( -0.315628f, 0.364455f, 0.766612f ),
			new Vector3f( -0.208888f, 0.281343f, 0.904355f ),
			new Vector3f( -0.113008f, 0.192607f, 1.033153f ),
			new Vector3f( -0.151708f, 0.431501f, 0.726908f ),
			new Vector3f( -0.097338f, 0.325036f, 0.878481f ),
			new Vector3f( -0.052798f, 0.216123f, 1.019227f ),
			new Vector3f( 0.028662f, 0.448942f, 0.71658f ),
			new Vector3f( 0.024652f, 0.335189f, 0.872468f ),
			new Vector3f( 0.013022f, 0.221544f, 1.016017f )
	);
	
	public static void onActivated(World world, BlockPos pos, EntityPlayer player) {
		float rotation = world.getBlockState(pos).getValue(BlockRotated.ROTATE) * -22.5f;
		
		// Last common function, x=a, y=b
		Ray lastRay = null;
		Ray firstRay = null;
		List<Ray> brbRayList = new ArrayList<Ray>();
		Vec3d lookVec = player.getLookVec();
		
		int button = -1;
		boolean breakLoop = false;
		
		for (int x=1; x<=dhdVertices.size()/3; x++) {
		//for (int x=1; x<=1; x++) {
			Ray currentRay;
			
			// Last run, current ray should be the first one
			if (x == dhdVertices.size()/3)
				currentRay = firstRay;
			else 
				currentRay = new Ray( getTransposedRay(x, rotation, pos, player) );
			
			// First run, we need to calculate the first right limiter function
			if (lastRay == null) {
				lastRay = firstRay = new Ray( getTransposedRay(0, rotation, pos, player) );	
				//brbRayList.add(firstRay);
			}
			
			List<Ray> transverseRays = new ArrayList<Ray>();
			
			for (int i=0; i<3; i++) {
				Ray r = new Ray( currentRay.getVert(i), lastRay.getVert(i) );
				transverseRays.add( r );
				
				if ( i == 2 ) {
					brbRayList.add( r );
				}
			}	
			
			for (int i=0; i<2; i++) {					
				Box box = new Box(currentRay, lastRay, transverseRays.get(i), transverseRays.get(i+1), i);
				
				if (box.checkForPointInBox( new Vector2f( (float)lookVec.x, (float)lookVec.z ) )) {
					button = x-1;
					if (i>0)
						button += 19;
					
					breakLoop = true;
					break;
				}
			}
			
			if (breakLoop)
				break;
			
			lastRay = currentRay;
		}
		
		if (button == -1) {				
			Box box = new Box( brbRayList );
			if (box.checkForPointInBox( new Vector2f( (float)lookVec.x, (float)lookVec.z ) )) {
				button = 38;
			}
		}
		
		if (button != -1) {
			//Aunis.info("Button: " + button);
			player.swingArm(EnumHand.MAIN_HAND);

			AunisPacketHandler.INSTANCE.sendToServer( new GateRenderingUpdatePacketToServer(EnumPacket.DHD_RENDERER_UPDATE, button, pos) );
			//AunisPacketHandler.INSTANCE.sendToServer( new GateRenderingUpdatePacketToServer(EnumPacket.Chevron, button, pos) );
		}
	}
	
	private static Vector2f getTransposed(Vector3f v, float rotation, BlockPos pos, EntityPlayer player) {
		DHDVertex current = new DHDVertex(v.x, v.y, v.z);

		return current.rotate( rotation ).localToGlobal(pos).calculateDiffrence(player).getViewport( player.getLookVec() );
	}
	
	private static List<Vector2f> getTransposedRay(int rayIndex, float rotation, BlockPos pos, EntityPlayer player) {
		List<Vector2f> out = new ArrayList<Vector2f>();
		
		for (int i=0; i<3; i++) {
			out.add( getTransposed( dhdVertices.get(rayIndex*3 + i), rotation, pos, player) );
		}
		
		return out;
	}
}