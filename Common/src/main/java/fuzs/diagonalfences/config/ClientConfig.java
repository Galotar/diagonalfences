package fuzs.diagonalfences.config;

import fuzs.puzzleslib.config.ConfigCore;
import fuzs.puzzleslib.config.annotation.Config;

public class ClientConfig implements ConfigCore {
    @Config(description = {"Enables very basic general integration for mods internally wrapping fence models, to allow those to also show diagonal connections.", "Only disable if this feature actively breaks fence rendering on a mod."})
    public boolean experimentalModIntegration = true;
    @Config(description = {"Provides integration for the Lambda Better Grass mod so that snowy/mossy/sculk fences will visually show diagonal connections.", "The \"experimental_mod_integration\" setting needs to be enables for this to apply.", "Only disable if this feature stops working due to breaking changes in Lambda Better Grass."})
    public boolean lambdaBetterGrassIntegration = true;
}
