/* 파일: DrivenInputRouter.java
 * 역할: FullPlaythroughDemo에서 실제 키보드·마우스 대신 경로 드라이버의 입력을 공급한다.
 * 주요 책임: GameplayScreen이 매 120Hz 틱마다 호출하는 consume()을 가로채 현재 스테이지
 *          드라이버의 InputFrame을 그대로 반환한다. 메뉴/정지 전환용 공개 필드는 그대로 둔다.
 * 협력 대상: FullPlaythroughDemo, GameplayScreen(소유), io.github.hookrecoil.input.InputRouter.
 * 관련 사양: 없음(신규 데모 전용 도구).
 */
package io.github.hookrecoil;

import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.input.InputRouter;

public final class DrivenInputRouter extends InputRouter {
    public interface Source {InputFrame next();}
    private Source source;
    public void setSource(Source source) {this.source=source;}
    @Override public InputFrame consume(float aimX,float aimY) {
        if(source==null)return super.consume(aimX,aimY);
        return source.next();
    }
}
