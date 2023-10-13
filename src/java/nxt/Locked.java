package nxt;

import java.util.Arrays;

import nxt.NxtException.NotValidException;
import nxt.util.Convert;

public class Locked {

  private static int THEFT_BLOCK_57MIL  = 282470;
  private static byte[][] locked_282470 = {
    Convert.parseHexString("2bf1ea03c87d1d4f10cc2b41fdd1d26e342cf9155e0e838c486fc4ea2708015b"),    
    Convert.parseHexString("e9be5d1247e3660708641aaa35461ee58201a6397db885ca726e409f4f402471"),    
    Convert.parseHexString("8a46ba6a538a4f50431625714b9716f7bcbf3973d7422821edda78b2e0514e43"),
    Convert.parseHexString("e77097f8b47de097396d39b1a486eb8fbdc90aa33d41f5850dc64826139c611c"),    
    Convert.parseHexString("8c22df4bcd6ef94890f943ecb975585fa186ef534465272e46ea52c3a4715c22"),    
    Convert.parseHexString("d107a7bb22b1edbfe1116066d3117ed2d5d2043a6d747881e0b7976fa2203e45"),    
    Convert.parseHexString("3e5b0d4d2915bd599a1d61f5c6952d435051f338776bc4eff0701686616f533e"),    
    Convert.parseHexString("31f6abbd336b35ca56e01031ab6661545f1f2775a33e369473fa126826de3059"),    
    Convert.parseHexString("0facf6460bfadb907d0840d7651860df99070490125e9e093ae7e1d3d3535c46"),    
    Convert.parseHexString("d3febb32e204c973b79ff60088cc9867b63d91ca3b8ac957feb3d2efda2b887c"),    
    Convert.parseHexString("37b901e70d65804610ec5a89e201a1a766f615d2d00a50b59b2dc3cfe60a0708"),    
    Convert.parseHexString("0a81f585a491a44336bc411d9a8136e3bbdb4dd402e23d08c05fce395b8f6d42"),    
    Convert.parseHexString("f9ee704d212f9f0faed0281945a16ded097ed36e4186ceaee9d3c3b476be4874"),    
    Convert.parseHexString("ff1d1110f0667379800d5710ab19686646c3adeab8249c43ba0e51921c9b6379"),    
    Convert.parseHexString("ddd749bc90e0173c66b94ba50a5462c8053c748e08482fffd5293d0f182dca38"),    
    Convert.parseHexString("428da144719172a1d9dde3b9e3fbfa91ece6864b6183ff969179b9193be04f27"),    
    Convert.parseHexString("5dfc17a13c9a846b51c1a85bd562238a0720a5f0417706fa4bf6d1d427da3012"),    
    Convert.parseHexString("a01c34975c321442c20f95ed92049e76baed6e899ef958fbfc695bf7abbe2703"),    
    Convert.parseHexString("d0d945bbe3ed38a0678f8a667b6a046feba85cbc85ac75e6f93e2c3b69ae3c25"),    
    Convert.parseHexString("06ced62face2d7ad38614d734b69b93dcea0c1acf7d94550bc26f21d7dd6b07b"),    
    Convert.parseHexString("f7cb778c484ee41a365f5b5d13629fbf4a2c637faa13c37b7ff70f301992f658"),    
    Convert.parseHexString("c5fe703df4f3ccc06b73730a5950dd661c4e46aa6a683884edeac0c14bb08377"),    
    Convert.parseHexString("4110b1a9095e789ed0663607e33fad191c190fdac6d65df76a8990f5cbfa7665"),    
    Convert.parseHexString("5284f8dd53880a5d0940445311c74e0893bbf751f4b4ec1182c404433ff58547"),    
    Convert.parseHexString("38ce37420a58f5f4cef7146026832f95068f6a69bb6b62fc7810e3803ba1f512"),    
    Convert.parseHexString("d4c35b2cf4ecb53bbbec2ba9bbf242b61cfd6938dc87d6c12df5965d9e4c881b"),    
    Convert.parseHexString("55c2e288b4304484b9af61933cb0b2a26ff596aae3567feda79ef92eb2066536"),    
    Convert.parseHexString("ac2996d2bdbe3db7fdc25d9d9c7309d603a00f44fb4336cc0eca30aa02115718"),    
    Convert.parseHexString("357567e7203ecf4d6fc103018b2e5fcc13f40e541cf085bb9730a84b635dde53"),    
    Convert.parseHexString("5647f4e7f84af2ef2b80fcfc6a9645e84fbd057bf39ec9ae0fc67efbc9cf1945"),   
    Convert.parseHexString("d891d8caf18fea2e3a754819d57d23cd1ae16bcee3282f3db81f802734f60d72"),    
    Convert.parseHexString("edab00ef934d5d0f4a6e1351b1c18fa8ac2cbedca5f9ec5901826947388f5052"),    
    Convert.parseHexString("ce1176be45edf494574e57b9b99a6489e0671523178fd0af089894217f11733a"),    
    Convert.parseHexString("2aafb707d9a69166d521558203c29a2fcdb28685a721ebf7965d810fc6aa9e01"),    
    Convert.parseHexString("562a329e1a8cc46b399c8f32807340e9cf2a010c78527d9ea1e130b16891a615"),    
    Convert.parseHexString("1fb8518724fb8a91a19d57ba0dbcd782914217d95d38efb65a14fbef9047a250"),    
    Convert.parseHexString("dc2d4afe37b7c9d222efe646e315450011867e7351227cc466bcfa2e3149c95a"),    
    Convert.parseHexString("9ecea55e761e8ed85a20366304d70d4c9828ce28f9769511c8299bebe1a58a2b"),    
    Convert.parseHexString("ada2f1a5a802be03a79350ade665e6ef7424c61d4c650a6119899fcc8f859a64"),    
    Convert.parseHexString("72005a736a3fe93c2e1ab5134415e60d3729e664260522c04bfb79dfb8aa7a79"),    
    Convert.parseHexString("cc20a835db02a48e9c6330d62018e98d697afb7049dffb98118563654536c03a"),    
    Convert.parseHexString("a75ae984a2fa5f0aa8b411a426bfcfc60f072bf54a93aba32401cad2081ccf70"),    
    Convert.parseHexString("3daed3949e62034a3f0f5adcccc9b9741ad0dfdc06d245623a431d1d9b67ff3b"),    
    Convert.parseHexString("45eeb60b64fe9cdc913fe7088e8a0fbef0a816c24204ed066038612ce83ac236"),    
    Convert.parseHexString("6f6b91a29b0076a789bfb22da05d1c67aa758378b77eb2d1c570d41559df9d39"),    
    Convert.parseHexString("118f19018ee38deda78725944a34b686a1dc7c2a25e794324eae08c1f81c8a67"),    
    Convert.parseHexString("883a525fbc51907933dcb5448568f94be0fb1ec7be1cfc32ae8feb2ff7a8971d"),    
    Convert.parseHexString("e1aede7ab95a9519e015476faa44313fd06d4b746dfe19f5b209ec5ef021b632"),    
    Convert.parseHexString("8f696dbc568ae82c38c87571fcd486d06544a12bb2122281e21cfef878b05552"),    
    Convert.parseHexString("cb2017432d84a41c6aaeb6b8f2589fdd84e65d0875b9e3e9ca3e456f89482e25"),    
    Convert.parseHexString("9d105552e6afcf68a276f0e5d4087938e28a46563e622e531e42f81c3aee835c"),    
    Convert.parseHexString("c18a4b31866c0bf6769940a4711a62005805747af0ce00916b1016556bce7d18"),    
    Convert.parseHexString("709d23d2d681c3c0566d292f41296462baa78191651a27a2cbc12f70b48b9479"),    
    Convert.parseHexString("f839d3a650b1a88ecb26cb3408afde3af3ab2e16073b805c655d683bb0b6272b"),    
    Convert.parseHexString("eb16b926ab96da5f0228cb77357ef7054a0cb682529f8019e05f5ed4b7254e5e"),    
    Convert.parseHexString("2e45340fb4d04c4776658a27424b54abe7cd7ec169203950dae5f6541997ae61"),    
    Convert.parseHexString("edb0231042ddea231aec321b4ee2408ee045b03087cc9c521794abf7a0df9448"),    
    Convert.parseHexString("271ca64483b4f79b358baf242f309968415d42c28e47b61d420aa41de694e840"),    
    Convert.parseHexString("2eed045000b4d824f4cac979f23b3a7ffc5a90b0d1e930db9d76b80f5a729e1f")
  };
  
  private static boolean hit(byte[] senderPublickey) {
    for (int i=0; i<locked_282470.length; i++) {
      if (Arrays.equals(senderPublickey, locked_282470[i])) {
        return true;
      }
    }
    return false;
  }

  public static void test(int height, byte[] senderPublickey) throws NotValidException {
    if (height > THEFT_BLOCK_57MIL && hit(senderPublickey)) {
      throw new NxtException.NotValidException("Public key locked for outgoing transactions");
    }    
  }
  
  public static boolean allowedToForge(byte[] senderPublickey) {
    return hit(senderPublickey) == false;
  }
}
