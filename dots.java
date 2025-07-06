import static java.lang.System.out;


 
static String hex(int v){
   var s = Integer.toBinaryString(v);
   return "00000000".substring(s.length())+s;

}


class IMG {
   int width;
   int height;
   byte[] bytes;
   int charWidth;
   int charHeight;
   char[] chars;
   IMG(int width, int height){
      this.width = width;
      this.height = height;
      this.bytes = new byte[width*height];
      this.charWidth = width/2;
      this.charHeight = height/4;
      this.chars = new char[charWidth*charHeight];
   }
   void set(int x, int y){
      bytes[(y*width)+x]=1;//0xff;
   }
   void reset(int x, int y){
      bytes[(y*width)+x]=0;
   }
   int i(int x, int y){
      byte b = bytes[(y*width)+x];
      return (int)(b<0?b+256:b);
   }
/**
See the unicode mapping table here 
https://images.app.goo.gl/ntxis4mKzn7GmrGb7
*/
   char brailchar(int bytebits){
       int mapped = (bytebits&0x07)|(bytebits&0x70)>>1|(bytebits&0x08)<<3|(bytebits&0x80);
       char brail = (char)(0x2800+mapped);
       return brail;
   }
   IMG home(){
     System.out.println("\033[0;0H");
     return this;
   }
   IMG delay(int ms){
     try{ Thread.sleep(ms); }catch(Throwable t){ }
     return this;
   }

   IMG clean(){
       Arrays.fill(bytes,(byte)0); 
       Arrays.fill(chars,(char)' '); 
       return this;
   }
   IMG map(){
      for (int cx = 0; cx<charWidth; cx++){
         for (int cy = 0; cy<charHeight; cy++){
            int bytebits=0;
            for (int dx=0;dx<2;dx++){
               for (int dy=0;dy<4;dy++){
                  bytebits|=i(cx*2+dx,cy*4+dy)<<(dx*4+dy);
               }
            } 
            chars[cy*charWidth+cx]=brailchar(bytebits);
         }
      }
      return this;
   }

   public String toString(){
      StringBuilder sb = new StringBuilder();
      sb.append("+");
      for (int i=0;i<charWidth; i++){
         sb.append("-");
      }
      sb.append("+\n|");
      for (int i=0;i<chars.length; i++){
         if (i>0 && (i%charWidth)==0){
           sb.append("|\n|");
         }
         sb.append(chars[i]);
      }
      sb.append("|\n+");
      for (int i=0;i<charWidth; i++){
         sb.append("-");
      }
      sb.append("+\n");
      return sb.toString();
   }
}

void main(String[] args) {

System.out.println("Modify your font on mac to Courier New - Regular - 14 v/i=81 n/n=81");
/*
  for (int i = 0x00;i<0x100;i++){
     char c = (char)(0x2800+i);
     if (i%16==0){
        print("\n "+ Integer.toHexString(c));
     }
     print('[');
     print(c);
     print(']');
  }
  print("\n");
*/

/*
  for (int i = 0x00;i<0x100;i++){
     int v1 = (i&1)|i&2|i&4|(i&16)>>1|(i&32)>>1|(i&64)>>1|(i&8)<<3|(i&128);
     int v = (i&0x07)|(i&0x70)>>1|(i&0x08)<<3|(i&0x80);


     char c = (char)(0x2800+v);
     if (i%16==0){
        print("\n "+ Integer.toHexString(c));
     }
     print('[');
     print(c);
     print(']');
  }
  print("\n");
  */

  IMG image = new IMG(128,128);

  for (int frame =0; frame<40; frame++){
     image.clean().home();
     for (int x=0; x<20; x++){
        image.set(frame+x,x);
        image.set(frame+20-x,frame+x);
     }
     for (int x=40; x<80; x++){
       for (int y=40; y<80; y++){
         image.set(x+frame,y+frame);
       }
     }
     System.out.println(image.map());
     image.delay(100);
  }
   
}
