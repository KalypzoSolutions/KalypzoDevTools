package fish.crafting.fimfabric.keybind;

public enum KeybindCategory {

    INTELLIJ("intellij"),
    IN_GAME_EDITOR("in_game_editor")
    ;

    private final String subid;

    KeybindCategory(String subid){
        this.subid = subid;
    }

    public String translation(){
        return "fim.keybind.category." + subid;
    }

    /**
     * Path of the {@code fim:<id>} identifier 26.2+ uses to register the category.
     */
    public String id(){
        return subid;
    }

}
