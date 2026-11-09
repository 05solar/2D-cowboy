/*
 * 파일: WindowsTaskbarIdentity.java
 * 역할: Windows 작업표시줄에서 HOOK & RECOIL을 Java 런타임과 분리된 앱으로 식별한다.
 * 주요 책임: 프로세스 및 GLFW 창의 AppUserModelID·작업표시줄 재실행 아이콘 속성을 등록한다.
 * 협력 대상: Lwjgl3Launcher, Windows Shell32, JNA.
 * 관련 사양: docs/10_PROLOGUE.md.
 */
package io.github.some_example_name.lwjgl3;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.COMInvoker;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Structure;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Applies Windows shell identity and the icon resource Windows uses for its taskbar group. */
final class WindowsTaskbarIdentity {
    private static final String APP_ID="05solar.HookRecoil";
    private static final GUID PROPERTY_STORE_IID=new GUID("{886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99}");
    private static final GUID APP_USER_MODEL_FORMAT=new GUID("{9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3}");
    private static final int APP_USER_MODEL_RELAUNCH_ICON=3,APP_USER_MODEL_ID=5,VT_LPWSTR=31;

    private WindowsTaskbarIdentity() {}

    static void applyToProcess() {
        if(!isWindows())return;
        try {
            HRESULT result=Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString(APP_ID));
            if(failed(result))System.err.println("Unable to set Windows taskbar process identity: "+result);
        } catch(Throwable failure) {
            System.err.println("Unable to set Windows taskbar process identity: "+failure.getMessage());
        }
    }

    static void applyToWindow(long windowHandle) {
        if(!isWindows()||windowHandle==0)return;
        Path icon=findWindowsIcon();
        if(icon==null) {System.err.println("Windows taskbar icon file was not found.");return;}
        try {
            PointerByReference storePointer=new PointerByReference();
            Function getPropertyStore=NativeLibrary.getInstance("shell32").getFunction(
                "SHGetPropertyStoreForWindow",Function.ALT_CONVENTION);
            PROPERTY_STORE_IID.write();
            int hr=getPropertyStore.invokeInt(new Object[]{new HWND(new Pointer(windowHandle)),PROPERTY_STORE_IID.getPointer(),storePointer});
            if(hr<0||storePointer.getValue()==null) {warn("SHGetPropertyStoreForWindow",hr);return;}
            PropertyStore store=new PropertyStore(storePointer.getValue());
            try {
                String iconResource=icon.toString()+",0";
                HRESULT iconResult=store.setValue(new PropertyKey(APP_USER_MODEL_RELAUNCH_ICON),iconResource);
                if(failed(iconResult)) {warn("RelaunchIconResource",iconResult.intValue());return;}
                HRESULT idResult=store.setValue(new PropertyKey(APP_USER_MODEL_ID),APP_ID);
                if(failed(idResult))warn("AppUserModelID",idResult.intValue());
            } finally {store.release();}
        } catch(Throwable failure) {
            System.err.println("Unable to set Windows taskbar window properties: "+failure.getMessage());
            failure.printStackTrace();
        }
    }

    private static Path findWindowsIcon() {
        Path directory=Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while(directory!=null) {
            Path candidate=directory.resolve("lwjgl3/icons/logo.ico");
            if(Files.isRegularFile(candidate))return candidate;
            directory=directory.getParent();
        }
        return null;
    }

    private static boolean isWindows(){return System.getProperty("os.name","").toLowerCase().startsWith("windows");}
    private static boolean failed(HRESULT result){return result==null||result.intValue()<0;}
    private static void warn(String operation,int hresult){System.err.println("Unable to set Windows taskbar "+operation+" (HRESULT 0x"+Integer.toHexString(hresult)+").");}

    private static final class PropertyStore extends COMInvoker {
        PropertyStore(Pointer pointer){setPointer(pointer);}
        HRESULT setValue(PropertyKey key,String value) {
            try(Memory text=new Memory((value.length()+1L)*Native.WCHAR_SIZE)) {
                text.setWideString(0,value);
                key.write();
                PropVariant variant=new PropVariant();variant.vt=VT_LPWSTR;variant.value=text;variant.write();
                return (HRESULT)_invokeNativeObject(6,new Object[]{getPointer(),key.getPointer(),variant.getPointer()},HRESULT.class);
            }
        }
        void release(){_invokeNativeInt(2,new Object[]{getPointer()});}
    }

    @Structure.FieldOrder({"format","id"})
    public static final class PropertyKey extends Structure {
        public GUID format;public int id;
        PropertyKey(int id){format=APP_USER_MODEL_FORMAT;this.id=id;}
    }

    @Structure.FieldOrder({"vt","reserved1","reserved2","reserved3","value"})
    public static final class PropVariant extends Structure {
        public short vt,reserved1,reserved2,reserved3;public Pointer value;
    }
}
