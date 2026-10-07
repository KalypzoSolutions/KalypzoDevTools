package fish.crafting.fimfabric.keybind;

public enum KeybindCategory {

    INTELLIJ("intellij"),
    IN_GAME_EDITOR("in_game_editor")
    ;

    private final String subid;

    KeybindCategory(String subid){
        this.subid = subid;
    }

    /**
     * Path of the {@code fim:<id>} identifier the category is registered under.
     */
    public String id(){
        return subid;
    }

}
