package na.eraka.utils.AvatarPackage;

public class SelectedAvatar {
    private String name="ic_avatar_default";   // Eraka 27 Oct 2026: neutral until chosen
    private static SelectedAvatar _SelectedAvatar;
    public synchronized static SelectedAvatar getInstance(){
        if(_SelectedAvatar == null){
            _SelectedAvatar = new SelectedAvatar();
        }
        return _SelectedAvatar;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
