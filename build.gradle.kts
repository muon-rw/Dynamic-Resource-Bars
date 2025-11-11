import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.annotationProcessor
import org.gradle.kotlin.dsl.modRuntimeOnly
import toni.blahaj.setup.*

plugins {
	id("toni.blahaj")
}

allprojects {
	configurations.all {

		/*
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
		*/
		exclude(group = "toni.txnilib")

	}
}

blahaj {
	config {
		// yarn()
		 versionedAccessWideners()
	}
	setup {
		//forgeConfig()

		// Bundled
		deps.compileOnly(deps.annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")!!)
		deps.implementation(deps.include("io.github.llamalad7:mixinextras-$loader:0.4.1")!!)

		// Required
		deps.modImplementation("fuzs.puzzleslib:puzzleslib-$loader:${property("puzzleslib")}")

		// Integrations
		// TODO: Mana: Ars Nouveau, Iron's Spellbooks, RPGMana, Mana Attributes, Archon?
		// Frostiful, Scorchful
		if (project.hasProperty("thermoo")) { deps.modCompileOnly("maven.modrinth:thermoo:${property("thermoo")}-$mc") }

		// Appleskin
		if (project.hasProperty("clothconfig")) { deps.modRuntimeOnly("me.shedaniel.cloth:cloth-config-$loader:${property("clothconfig")}") }
		deps.modCompileOnly("squeek.appleskin:appleskin-$loader:${property("appleskin")}")

		// Farmer's Delight
		if (project.hasProperty("farmersdelight_forge")) { deps.modCompileOnly("maven.modrinth:farmers-delight:$mc-${property("farmersdelight_forge")}-$loader") }
		if (project.hasProperty("farmersdelight_fabric")) { deps.modCompileOnly("vectorwing:FarmersDelight:$mc-${property("farmersdelight_fabric")}+refabricated") }

		// Bewitchment
		if (project.hasProperty("bewitchment")) { deps.modCompileOnly("maven.modrinth:bewitchment:${property("bewitchment")}") }

		// Ars Nouveau
		if (project.hasProperty("ars_nouveau")) { deps.modCompileOnly("com.hollingsworth.ars_nouveau:ars_nouveau-$mc:${property("ars_nouveau")}") }

		// Iron's Spellbooks
		if (project.hasProperty("irons_spellbooks")) { deps.modCompileOnly("io.redspace:irons_spellbooks:${property("irons_spellbooks")}") }
		if (project.hasProperty("ironsspellbooks")) { deps.modCompileOnly("io.redspace.ironsspellbooks:irons_spellbooks:${property("ironsspellbooks")}") }

		// RPGMana + Mana Attributes
		if (project.hasProperty("spell_engine")) { deps.modCompileOnly("maven.modrinth:spell-engine:${property("spell_engine")}-fabric")}
		if (project.hasProperty("rpgmana")) { deps.modCompileOnly("curse.maven:rpgmana-1021902:${property("rpgmana")}")}
		if (project.hasProperty("mana_attributes")) { deps.modCompileOnly("maven.modrinth:mana-attributes:${property("mana_attributes")}") }

		// Stamina Attribute
		if (project.hasProperty("stamina_attributes")) { deps.modCompileOnly("maven.modrinth:stamina-attributes:${property("stamina_attributes")}") }

		// Publishing
		addRequiredMod("puzzles-lib")
			.modrinth("puzzles-lib") // override with Modrinth URL slug
			.addPlatform("1.21.1-neoforge", "v21.1.36-1.21.1-NeoForge") { required() }
			.addPlatform("1.21.1-fabric", "v21.1.36-1.21.1-Fabric") { required() }
			.addPlatform("1.20.1-forge", "v8.1.32-1.20.1-Forge") { required() }
			.addPlatform("1.20.1-fabric", "v8.1.32-1.20.1-Fabric") { required() }
	}
}

repositories {
	maven("https://maven.ryanliptak.com/" ) // Appleskin
	maven("https://maven.ladysnake.org/releases")
	maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
	maven("https://maven.shedaniel.me/")
	maven("https://maven.terraformersmc.com/releases/")
	maven("https://maven.kosmx.dev/") // Player Animator (required by Iron's)
	maven("https://maven.blamejared.com/") // Ars Nouveau
	maven("https://maven.theillusivec4.top/") // Curios (required by Ars + Iron's)
	maven("https://maven.minecraftforge.net/") // Terrablender (required by Ars)

	// Iron's Spellbooks
	maven {
		url = uri("https://code.redspace.io/releases")
		content {
			includeGroupByRegex("io.redspace.*")
		}
	}
	maven {
		url = uri("https://code.redspace.io/snapshots")
		content {
			includeGroupByRegex("io.redspace.*")
		}
	}

	// Farmer's Delight + Refabricated
	maven("https://maven.greenhouse.lgbt/releases/")
	maven("https://maven.greenhouse.lgbt/snapshots/")
	maven("https://mvn.devos.one/releases/")
	maven {
		url = uri("https://maven.jamieswhiteshirt.com/libs-release")
		content {
			includeGroup("com.jamieswhiteshirt")
		}
	}
	maven {
		url = uri("https://jitpack.io/")
		content {
			excludeGroup("io.github.fabricators_of_create")
		}
	}
}