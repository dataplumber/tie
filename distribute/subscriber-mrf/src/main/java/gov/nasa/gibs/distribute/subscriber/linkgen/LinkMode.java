package gov.nasa.gibs.distribute.subscriber.linkgen;

public enum LinkMode {
   SCI("SCI"), NRT("NRT");
   
   String val;
   
   private LinkMode(String val) {
      this.val = val;
   }
   
   public String getValue() {
      return this.val;
   }
}
