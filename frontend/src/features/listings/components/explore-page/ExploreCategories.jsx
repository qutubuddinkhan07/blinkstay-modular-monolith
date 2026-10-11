import React from "react";
import {
  FiGrid,
  FiSun,
  FiCompass,
  FiNavigation,
  FiLayers,
  FiAward,
  FiDollarSign,
  FiHome,
  FiCoffee,
  FiBox,
  FiTrendingUp,
  FiShield,
  FiAnchor,
  FiCloudSnow,
  FiFeather,
} from "react-icons/fi";

const CATEGORIES = [
  { label: "All", value: "", icon: FiGrid },
  { label: "Farms", value: "FARMS", icon: FiFeather },
  { label: "Mountains", value: "MOUNTAINS", icon: FiCompass },
  { label: "Trending", value: "TRENDING", icon: FiTrendingUp },
  { label: "Boats", value: "BOATS", icon: FiAnchor },
  { label: "Beach", value: "BEACH", icon: FiSun },
  { label: "City", value: "CITY", icon: FiNavigation },
  { label: "Countryside", value: "COUNTRYSIDE", icon: FiLayers },
  { label: "Luxury", value: "LUXURY", icon: FiAward },
  { label: "Budget", value: "BUDGET", icon: FiDollarSign },
  { label: "Villas", value: "VILLA", icon: FiHome },
  { label: "Hotels", value: "HOTEL", icon: FiCoffee },
  { label: "Rooms", value: "ROOM", icon: FiBox },
  { label: "Castles", value: "CASTLES", icon: FiShield },
  { label: "Arctic", value: "ARCTIC", icon: FiCloudSnow },
];

const ExploreCategories = ({ selectedCategory, onSelectCategory }) => {
  return (
    <div className="border-b border-border bg-bg py-3 shadow-sm">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-10">
        <div className="flex items-center gap-8 overflow-x-auto pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            const isSelected =
              selectedCategory === cat.value ||
              (!selectedCategory && cat.value === "");

            return (
              <button
                key={cat.value}
                type="button"
                onClick={() => onSelectCategory(cat.value)}
                className={`flex flex-col items-center gap-2 border-b-2 pb-2 pt-1 transition-colors shrink-0 cursor-pointer ${
                  isSelected
                    ? "border-text text-text font-semibold"
                    : "border-transparent text-subtext hover:border-subtext hover:text-text"
                }`}
              >
                <Icon size={20} />
                <span className="text-xs whitespace-nowrap">{cat.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default ExploreCategories;
