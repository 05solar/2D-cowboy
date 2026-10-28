/* 파일: BuildPrologueFont.java
 * 역할: 둥근모꼴+ Fixedsys 비트맵 글꼴 생성. 책임: 실제 UI 문자 수집 및 BMFont 출력.
 * 협력: HudView, 스테이지 데이터, DungGeunMo.ttf. 수명: 일회성 개발 도구. 관련 사양: docs/10_PROLOGUE.md.
 */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;

public class BuildPrologueFont {
    public static void main(String[] args) throws Exception {
        TreeSet<Character> chars=new TreeSet<>();
        for(char c=32;c<127;c++)chars.add(c);
        java.util.List<Path> paths=new ArrayList<>();
        try(java.util.stream.Stream<Path> stream=Files.walk(Paths.get("core/src/main/java"))) {
            stream.filter(p->p.toString().endsWith(".java")).forEach(paths::add);
        }
        try(java.util.stream.Stream<Path> stream=Files.walk(Paths.get("assets/stages"))) {
            stream.filter(p->p.toString().endsWith(".json")).forEach(paths::add);
        }
        paths.add(Paths.get("assets/cave_chapter/stages.json"));
        for(Path path:paths) for(char c:new String(Files.readAllBytes(path),StandardCharsets.UTF_8).toCharArray())
            if(c>=32 && c!='\uFEFF')chars.add(c);
        Font source=Font.createFont(Font.TRUETYPE_FONT,new File("assets/ui/fonts/DungGeunMo.ttf"));
        Font font=source.deriveFont(Font.PLAIN,64f);
        for(char c:chars)if(!font.canDisplay(c))throw new IllegalArgumentException("DungGeunMo has no glyph for U+"+Integer.toHexString(c));
        // DungGeunMo is rendered at 64 px. A 48 px cell made Hangul glyphs
        // overlap neighboring cells and produced visibly corrupted text.
        int columns=40,cell=80,rows=(chars.size()+columns-1)/columns;
        BufferedImage image=new BufferedImage(columns*cell,rows*cell,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setFont(font);g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        FontMetrics metrics=g.getFontMetrics();
        StringBuilder out=new StringBuilder("info face=\"DungGeunMo\" size=64 bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=0 aa=0 padding=0,0,0,0 spacing=0,0\n");
        out.append("common lineHeight=").append(metrics.getHeight()).append(" base=").append(metrics.getAscent()).append(" scaleW=").append(image.getWidth()).append(" scaleH=").append(image.getHeight()).append(" pages=1 packed=0\npage id=0 file=\"prologue.png\"\nchars count=").append(chars.size()).append('\n');
        int index=0;
        for(char c:chars) {
            int x=(index%columns)*cell,y=(index/columns)*cell,width=metrics.charWidth(c);
            g.drawString(String.valueOf(c),x+2,y+metrics.getAscent()+2);
            Rectangle bounds=font.createGlyphVector(g.getFontRenderContext(),String.valueOf(c)).getPixelBounds(g.getFontRenderContext(),0,0);
            if(bounds.width+4>cell || bounds.height+4>cell)
                throw new IllegalStateException("Glyph does not fit atlas cell: U+"+Integer.toHexString(c)+" "+bounds);
            out.append("char id=").append((int)c).append(" x=").append(x+2+bounds.x).append(" y=").append(y+metrics.getAscent()+2+bounds.y)
                .append(" width=").append(bounds.width).append(" height=").append(bounds.height)
                .append(" xoffset=").append(bounds.x).append(" yoffset=").append(metrics.getAscent()+bounds.y)
                .append(" xadvance=").append(width+1).append(" page=0 chnl=15\n");
            index++;
        }
        g.dispose();Path dest=Paths.get("assets/ui/fonts");Files.createDirectories(dest);
        ImageIO.write(image,"png",dest.resolve("prologue.png").toFile());
        Files.write(dest.resolve("prologue.fnt"),out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Font generated: "+chars.size()+" glyphs, "+font.getFontName());
    }
}
