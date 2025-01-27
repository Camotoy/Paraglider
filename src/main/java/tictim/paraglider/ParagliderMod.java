package tictim.paraglider;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.ScreenManager.IScreenFactory;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import tictim.paraglider.capabilities.Paraglider;
import tictim.paraglider.capabilities.PlayerMovement;
import tictim.paraglider.capabilities.Stamina;
import tictim.paraglider.client.screen.StatueBargainScreen;
import tictim.paraglider.contents.Contents;
import tictim.paraglider.contents.ModVillageStructures;
import tictim.paraglider.event.ParagliderClientEventHandler;
import tictim.paraglider.item.ParagliderItem;
import tictim.paraglider.network.ModNet;
import tictim.paraglider.recipe.ConfigConditionSerializer;
import tictim.paraglider.recipe.bargain.StatueBargainContainer;
import tictim.paraglider.wind.Wind;

import javax.annotation.Nullable;
import javax.naming.OperationNotSupportedException;

@Mod(ParagliderMod.MODID)
@Mod.EventBusSubscriber(modid = ParagliderMod.MODID, bus = Bus.MOD)
public class ParagliderMod{
	public static final String MODID = "paraglider";
	public static final Logger LOGGER = LogManager.getLogger("Paraglider");

	public ParagliderMod(){
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
		Contents.registerEventHandlers(eventBus);
		ModCfg.init();
		ModNet.init();
		for(ConfigConditionSerializer c : ConfigConditionSerializer.values()) CraftingHelper.register(c);
	}

	@SubscribeEvent
	public static void setup(FMLCommonSetupEvent event){
		event.enqueueWork(() -> {
			registerDefaultCapability(PlayerMovement.class);
			registerDefaultCapability(Paraglider.class);
			registerDefaultCapability(Wind.class);
			registerDefaultCapability(Stamina.class);

			ModVillageStructures.addVillageStructures();
		});
	}

	private static <T> void registerDefaultCapability(Class<T> classOf){
		CapabilityManager.INSTANCE.register(classOf, new Capability.IStorage<T>(){
			@Nullable @Override public INBT writeNBT(Capability<T> capability, T instance, Direction side){
				return null;
			}
			@Override public void readNBT(Capability<T> capability, T instance, Direction side, INBT nbt){}
		}, () -> {
			throw new OperationNotSupportedException();
		});
	}

	@SubscribeEvent
	public static void onEntityAttributeModification(EntityAttributeModificationEvent event){
		event.add(EntityType.PLAYER, Contents.MAX_STAMINA.get());
	}

	@Mod.EventBusSubscriber(modid = ParagliderMod.MODID, bus = Bus.MOD, value = Dist.CLIENT)
	private static final class ClientHandler{
		private ClientHandler(){}

		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event){
			event.enqueueWork(() -> {
				IItemPropertyGetter itemPropertyGetter = (stack, world, entity) -> entity instanceof PlayerEntity&&ParagliderItem.isItemParagliding(stack) ? 1 : 0;

				ItemModelsProperties.registerProperty(Contents.PARAGLIDER.get(), new ResourceLocation("paragliding"), itemPropertyGetter);
				ItemModelsProperties.registerProperty(Contents.DEKU_LEAF.get(), new ResourceLocation("paragliding"), itemPropertyGetter);

				IScreenFactory<StatueBargainContainer, StatueBargainScreen> f = StatueBargainScreen::new;
				ScreenManager.registerFactory(Contents.GODDESS_STATUE_CONTAINER.get(), f);
				ScreenManager.registerFactory(Contents.HORNED_STATUE_CONTAINER.get(), f);

				RenderTypeLookup.setRenderLayer(Contents.RITO_GODDESS_STATUE.get(), RenderType.getCutout());

				KeyBinding paragliderSettingsKey = new KeyBinding(
						"key.paraglider.paragliderSettings",
						KeyConflictContext.IN_GAME,
						KeyModifier.CONTROL,
						InputMappings.Type.KEYSYM,
						GLFW.GLFW_KEY_P, "key.categories.misc");
				ClientRegistry.registerKeyBinding(paragliderSettingsKey);
				ParagliderClientEventHandler.setParagliderSettingsKey(paragliderSettingsKey);
			});
		}

		@SubscribeEvent
		public static void addColorHandler(ColorHandlerEvent.Item event){
			event.getItemColors().register((stack, tint) -> tint>0 ? -1 : ((IDyeableArmorItem)stack.getItem()).getColor(stack),
					Contents.PARAGLIDER.get(),
					Contents.DEKU_LEAF.get());
		}
	}
}
