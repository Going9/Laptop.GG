(() => {
    const budgetInput = document.getElementById("budget");
    const budgetDisplay = document.getElementById("budgetDisplay");
    const budgetToggleValue = document.getElementById("budgetToggleValue");
    const weightInput = document.getElementById("weight");
    const weightDisplay = document.getElementById("weightDisplay");
    const weightToggleValue = document.getElementById("weightToggleValue");
    const screenSizeGrid = document.getElementById("screenSizeGrid");
    const screenSizeSelectionSummary = document.getElementById("screenSizeSelectionSummary");

    if (
        !budgetInput ||
        !budgetDisplay ||
        !budgetToggleValue ||
        !weightInput ||
        !weightDisplay ||
        !weightToggleValue ||
        !screenSizeGrid ||
        !screenSizeSelectionSummary
    ) {
        return;
    }

    function formatBudget(value) {
        return !Number.isNaN(Number(value)) && value !== ""
            ? `${Number(value).toLocaleString()}원`
            : "0원";
    }

    function updateBudgetDisplay() {
        const formattedValue = formatBudget(budgetInput.value);

        budgetDisplay.textContent = `현재 예산: ${formattedValue}`;
        budgetToggleValue.textContent = `현재 예산: ${formattedValue}`;

        syncPresetMenu("budgetPresetMenu", budgetInput.value);
    }

    function updateWeightDisplay() {
        const value = weightInput.value;
        const formattedValue = !Number.isNaN(Number(value)) && value !== ""
            ? `${value}kg`
            : "0kg";

        weightDisplay.textContent = `현재 무게: ${formattedValue}`;
        weightToggleValue.textContent = `현재 무게: ${formattedValue}`;

        syncPresetMenu("weightPresetMenu", value);
    }

    function applyBudgetPreset(value) {
        budgetInput.value = value;

        updateBudgetDisplay();
        closePresetPicker("budgetPicker");
    }

    function applyWeightPreset(value) {
        weightInput.value = value;

        updateWeightDisplay();
        closePresetPicker("weightPicker");
    }

    function syncPresetMenu(menuId, value) {
        document.querySelectorAll(`#${menuId} .preset-dropdown-pill`).forEach((item) => {
            item.classList.toggle("is-active", item.dataset.value === value);
        });
    }

    function closePresetPicker(pickerId) {
        const picker = document.getElementById(pickerId);
        if (!picker) {
            return;
        }

        picker.open = false;
    }

    function syncScreenSizeSelection() {
        const selectedMode = document.querySelector("input[name='screenSizeMode']:checked")?.value;
        const checkedLabels = [];

        document.querySelectorAll(".size-chip").forEach((label) => {
            const input = label.querySelector("input[type='checkbox']");
            const isChecked = input.checked;
            label.classList.toggle("is-selected", isChecked);
            if (isChecked) {
                checkedLabels.push(label.querySelector("span").textContent.trim());
            }
        });

        if (selectedMode === "ANY") {
            screenSizeSelectionSummary.textContent = "선택한 화면 크기: 상관없음";
            return;
        }

        if (selectedMode === "NOT_SURE") {
            screenSizeSelectionSummary.textContent = "선택한 화면 크기: 잘 모르겠어요";
            return;
        }

        screenSizeSelectionSummary.textContent = checkedLabels.length > 0
            ? `선택한 화면 크기: ${checkedLabels.join(", ")}`
            : "선택한 화면 크기: 아직 선택하지 않았어요";
    }

    function syncScreenSizeMode() {
        const selectedMode = document.querySelector("input[name='screenSizeMode']:checked")?.value;
        const sizeInputs = screenSizeGrid.querySelectorAll("input[type='checkbox']");
        const isSelectMode = selectedMode === "SELECT";

        screenSizeGrid.classList.toggle("is-disabled", !isSelectMode);
        sizeInputs.forEach((input) => {
            input.disabled = !isSelectMode;
        });

        syncScreenSizeSelection();
    }

    budgetInput.addEventListener("input", updateBudgetDisplay);
    weightInput.addEventListener("input", updateWeightDisplay);

    document.querySelectorAll("#budgetPresetMenu .preset-dropdown-pill").forEach((button) => {
        button.addEventListener("click", () => applyBudgetPreset(button.dataset.value));
    });

    document.querySelectorAll("#weightPresetMenu .preset-dropdown-pill").forEach((button) => {
        button.addEventListener("click", () => applyWeightPreset(button.dataset.value));
    });

    document.querySelectorAll("input[name='screenSizeMode']").forEach((input) => {
        input.addEventListener("change", syncScreenSizeMode);
    });

    document.querySelectorAll(".size-chip input[type='checkbox']").forEach((input) => {
        input.addEventListener("change", syncScreenSizeSelection);
    });

    updateBudgetDisplay();
    updateWeightDisplay();
    syncScreenSizeMode();
})();
