import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { setNavigate } from "../utils/navigate";

// A tiny component that lives inside the router and registers navigate
const NavigateSetter = () => {
  const navigate = useNavigate();

  useEffect(() => {
    setNavigate(navigate);
  }, [navigate]);

  return null; // renders nothing
};

export default NavigateSetter;
