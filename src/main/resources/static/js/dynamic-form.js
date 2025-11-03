// Глобальные переменные
let formConfig = null;
let formData = {};
let calculatedValues = {};
let currentAvailableTemplates = [];
let fieldValuesHistory = {}; // История значений для полей
let selectedTemplatesForArchive = new Set(); // Выбранные шаблоны для архива

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadFormConfig();
    setupEventListeners();
    
    // Предотвращаем отправку формы
    const form = document.getElementById('dynamicForm');
    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            return false;
        });
    }
});

// ========== ЗАГРУЗКА КОНФИГУРАЦИИ ==========

async function loadFormConfig() {
    try {
        const [configResponse, historyResponse] = await Promise.all([
            fetch('/api/form-config'),
            fetch('/api/field-values-history')
        ]);
        formConfig = await configResponse.json();
        fieldValuesHistory = await historyResponse.json();
        renderForm();
    } catch (error) {
        console.error('Ошибка загрузки конфигурации:', error);
        showNotification('Помилка завантаження конфігурації', 'error');
    }
}

// ========== ОТОБРАЖЕНИЕ ФОРМЫ ==========

function renderForm() {
    const formFields = document.getElementById('formFields');
    
    if (!formConfig.fields || formConfig.fields.length === 0) {
        formFields.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📝</div>
                <div class="empty-state-text">Немає налаштованих полів</div>
                <div class="empty-state-hint">Перейдіть у налаштування для створення полів</div>
            </div>
        `;
        return;
    }
    
    // Сортируем поля по order
    const sortedFields = [...formConfig.fields].sort((a, b) => a.order - b.order);
    
    formFields.innerHTML = sortedFields.map(field => renderField(field)).join('');
    
    // Добавляем обработчики событий
    setupFieldEventListeners();
}

function renderField(field) {
    const fieldId = `field_${field.id}`;
    const isCalculated = field.fieldType === 'formula';
    
    let fieldHtml = '';
    
        if (isCalculated) {
        // Поле с формулой - только для отображения
        fieldHtml = `
            <div class="formula-field">
                <label class="field-label">${field.displayName}</label>
                <div class="formula-preview">${field.formula}</div>
                <div class="calculated-value" id="calculated_${field.id}">Обчислюється автоматично...</div>
            </div>
        `;
    } else {
        // Обычное поле ввода
        const required = field.required ? '<span class="required">*</span>' : '';
        const inputType = getInputType(field.fieldType);
        
        fieldHtml = `
            <div class="form-group">
                <label for="${fieldId}">${field.displayName} ${required}</label>
                ${renderInput(field, fieldId)}
                <small class="field-hint">${getFieldHint(field)}</small>
            </div>
        `;
    }
    
    return fieldHtml;
}

function renderInput(field, fieldId) {
    const fieldType = field.fieldType;
    const hasHistory = field.rememberValues && fieldValuesHistory[field.id] && fieldValuesHistory[field.id].length > 0;
    
    // Если есть история, создаем input с кастомным dropdown (только для input элементов, не для textarea)
    if (hasHistory && fieldType !== 'textarea') {
        const history = fieldValuesHistory[field.id];
        const dropdownId = `dropdown_${field.id}`;
        
        let inputHtml = '';
        switch (fieldType) {
            case 'date':
                inputHtml = `<input type="date" id="${fieldId}" name="${field.placeholder}" autocomplete="off">`;
                break;
            case 'number':
                inputHtml = `<input type="number" id="${fieldId}" name="${field.placeholder}" step="any" placeholder="Введіть число" autocomplete="off">`;
                break;
            default: // text
                inputHtml = `<input type="text" id="${fieldId}" name="${field.placeholder}" placeholder="Введіть ${field.displayName.toLowerCase()}" autocomplete="off">`;
        }
        
        // Кастомный dropdown с кнопками удаления
        const dropdownHtml = `
            <div class="history-dropdown-container">
                <button type="button" class="history-dropdown-toggle" onclick="toggleHistoryDropdown('${field.id}')" title="Показати історію">
                    📋
                </button>
                <div id="${dropdownId}" class="history-dropdown">
                    ${history.map(value => `
                        <div class="history-item">
                            <span class="history-value" onclick="selectHistoryValue('${field.id}', '${escapeHtml(value)}')">${escapeHtml(value)}</span>
                            <button type="button" class="history-delete-btn" onclick="deleteHistoryValue('${field.id}', '${escapeHtml(value)}', event)" title="Видалити">×</button>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
        return `<div class="input-with-history">${inputHtml}${dropdownHtml}</div>`;
    }
    
    // Обычные поля без истории
    switch (fieldType) {
        case 'textarea':
            return `<textarea id="${fieldId}" name="${field.placeholder}" rows="3" placeholder="Введіть ${field.displayName.toLowerCase()}"></textarea>`;
        
        case 'date':
            return `<input type="date" id="${fieldId}" name="${field.placeholder}">`;
        
        case 'number':
            return `<input type="number" id="${fieldId}" name="${field.placeholder}" step="any" placeholder="Введіть число">`;
        
        default: // text
            return `<input type="text" id="${fieldId}" name="${field.placeholder}" placeholder="Введіть ${field.displayName.toLowerCase()}">`;
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== УПРАВЛЕНИЕ ИСТОРИЕЙ ЗНАЧЕНИЙ ==========

function toggleHistoryDropdown(fieldId) {
    const dropdown = document.getElementById(`dropdown_${fieldId}`);
    const isVisible = dropdown.style.display === 'block';
    
    // Закрываем все другие dropdown
    document.querySelectorAll('.history-dropdown').forEach(d => {
        if (d.id !== `dropdown_${fieldId}`) {
            d.style.display = 'none';
        }
    });
    
    dropdown.style.display = isVisible ? 'none' : 'block';
}

function selectHistoryValue(fieldId, value) {
    const input = document.getElementById(`field_${fieldId}`);
    if (input) {
        input.value = value;
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }
    
    // Закрываем dropdown
    const dropdown = document.getElementById(`dropdown_${fieldId}`);
    if (dropdown) {
        dropdown.style.display = 'none';
    }
}

async function deleteHistoryValue(fieldId, value, event) {
    event.stopPropagation();
    
    if (!confirm(`Видалити значення "${value}" з історії?`)) {
        return;
    }
    
    try {
        const encodedValue = encodeURIComponent(value);
        const response = await fetch(`/api/field-values-history/${fieldId}?value=${encodedValue}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            // Обновляем локальную историю
            if (fieldValuesHistory[fieldId]) {
                const index = fieldValuesHistory[fieldId].indexOf(value);
                if (index > -1) {
                    fieldValuesHistory[fieldId].splice(index, 1);
                    
                    // Если история пустая, удаляем поле
                    if (fieldValuesHistory[fieldId].length === 0) {
                        delete fieldValuesHistory[fieldId];
                    }
                }
            }
            
            // Перерисовываем форму
            renderForm();
            showNotification('Значення видалено з історії', 'success');
        } else {
            showNotification('Помилка видалення значення', 'error');
        }
    } catch (error) {
        console.error('Помилка видалення:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    }
}

// Закрытие dropdown при клике вне его
document.addEventListener('click', (e) => {
    if (!e.target.closest('.history-dropdown-container')) {
        document.querySelectorAll('.history-dropdown').forEach(d => {
            d.style.display = 'none';
        });
    }
});

function getInputType(fieldType) {
    switch (fieldType) {
        case 'date': return 'date';
        case 'number': return 'number';
        case 'textarea': return 'textarea';
        default: return 'text';
    }
}

function getFieldHint(field) {
    if (field.fieldType === 'formula') {
        return 'Це поле обчислюється автоматично за формулою';
    }
    return field.required ? 'Обов\'язкове поле' : 'Опціональне поле';
}

// ========== ОБРАБОТЧИКИ СОБЫТИЙ ==========

function setupEventListeners() {
    document.getElementById('checkAvailabilityBtn').addEventListener('click', checkDocumentAvailability);
    document.getElementById('clearFormBtn').addEventListener('click', clearForm);
}

function setupFieldEventListeners() {
    // Обработчики для всех полей ввода
    document.querySelectorAll('input, textarea').forEach(input => {
        input.addEventListener('input', handleFieldChange);
        input.addEventListener('change', handleFieldChange);
    });
}

function handleFieldChange(event) {
    const fieldName = event.target.name;
    const fieldValue = event.target.value;
    
    // Обновляем данные формы
    formData[fieldName] = fieldValue;
    
    // Вычисляем формулы
    calculateFormulas();
    
    // Проверяем доступность кнопки
    updateCheckButton();
}

// ========== ВЫЧИСЛЕНИЕ ФОРМУЛ ==========

async function calculateFormulas() {
    try {
        const response = await fetch('/api/calculate-formulas', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        
        if (response.ok) {
            calculatedValues = await response.json();
            updateCalculatedFields();
        }
    } catch (error) {
        console.error('Ошибка вычисления формул:', error);
    }
}

function updateCalculatedFields() {
    formConfig.fields
        .filter(field => field.fieldType === 'formula')
        .forEach(field => {
            const element = document.getElementById(`calculated_${field.id}`);
            if (element) {
                const value = calculatedValues[field.placeholder] || 'Помилка обчислення';
                element.textContent = value;
            }
        });
}

// ========== ПРОВЕРКА ДОСТУПНОСТИ ДОКУМЕНТОВ ==========

async function checkDocumentAvailability() {
    try {
        const response = await fetch('/api/check-template-availability', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        
        if (response.ok) {
            const result = await response.json();
            currentAvailableTemplates = result.availableTemplates;
            renderAvailableDocuments(result.availableTemplates, result.allTemplates);
        }
    } catch (error) {
        console.error('Ошибка проверки доступности:', error);
        showNotification('Помилка перевірки документів', 'error');
    }
}

function renderAvailableDocuments(availableTemplates, allTemplates) {
    const documentsSection = document.getElementById('availableDocuments');
    const documentsList = document.getElementById('documentsList');
    
    documentsSection.style.display = 'block';
    documentsSection.classList.add('fade-in');
    
    // Удаляем из выбора шаблоны, которые больше не доступны
    const availableTemplateIds = new Set(availableTemplates.map(t => t.id));
    selectedTemplatesForArchive.forEach(templateId => {
        if (!availableTemplateIds.has(templateId)) {
            selectedTemplatesForArchive.delete(templateId);
        }
    });
    
    const documentsHtml = allTemplates.map(template => {
        const isAvailable = availableTemplates.some(t => t.id === template.id);
        
        return `
            <div class="document-card-compact ${isAvailable ? 'available' : 'unavailable'}">
                <div class="document-header-compact">
                    <span class="document-name-compact">${template.displayName}</span>
                </div>
                <div class="document-controls-compact">
                    <div class="archive-checkboxes">
                        <label class="archive-checkbox-label" title="Додати оригінал в архів">
                            <input type="checkbox" 
                                   class="archive-checkbox" 
                                   id="archive_original_${template.id}"
                                   ${!isAvailable ? 'disabled' : ''}
                                   onchange="updateArchiveSelection('${template.id}')">
                            <span class="checkbox-label-text">Оригінал</span>
                        </label>
                        <label class="archive-checkbox-label" title="Додати PDF в архів">
                            <input type="checkbox" 
                                   class="archive-checkbox" 
                                   id="archive_pdf_${template.id}"
                                   ${!isAvailable ? 'disabled' : ''}
                                   onchange="updateArchiveSelection('${template.id}')">
                            <span class="checkbox-label-text">PDF</span>
                        </label>
                    </div>
                    <div class="download-buttons">
                        <button type="button" 
                                class="document-btn-compact ${isAvailable ? 'available' : 'unavailable'}" 
                                onclick="${isAvailable ? `downloadDocument('${template.id}', false)` : 'void(0)'}"
                                ${!isAvailable ? 'disabled' : ''}
                                title="${isAvailable ? 'Завантажити оригінал' : 'Недоступний'}">
                            📄 DOCX/XLSX
                        </button>
                        <button type="button" 
                                class="document-btn-compact btn-pdf ${isAvailable ? 'available' : 'unavailable'}" 
                                onclick="${isAvailable ? `downloadDocument('${template.id}', true)` : 'void(0)'}"
                                ${!isAvailable ? 'disabled' : ''}
                                title="${isAvailable ? 'Завантажити PDF' : 'Недоступний'}">
                            📄 PDF
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    // Добавляем кнопку архива если есть доступные документы
    let archiveButton = '';
    if (availableTemplates.length > 0) {
        archiveButton = `
            <div class="archive-section">
                <button type="button" 
                        class="btn btn-archive" 
                        onclick="downloadArchive()"
                        id="archiveBtn">
                    📦 Завантажити вибрані документи (ZIP)
                </button>
                <p class="archive-hint" id="archiveHint">Оберіть документи для архіву</p>
            </div>
        `;
    }
    
    documentsList.innerHTML = documentsHtml + archiveButton;
    updateArchiveButtonState();
}

function updateArchiveSelection(templateId) {
    const originalChecked = document.getElementById(`archive_original_${templateId}`)?.checked;
    const pdfChecked = document.getElementById(`archive_pdf_${templateId}`)?.checked;
    
    // Если хотя бы один чекбокс отмечен, документ считается выбранным
    if (originalChecked || pdfChecked) {
        selectedTemplatesForArchive.add(templateId);
    } else {
        selectedTemplatesForArchive.delete(templateId);
    }
    
    updateArchiveButtonState();
}

function updateArchiveButtonState() {
    const archiveBtn = document.getElementById('archiveBtn');
    const archiveHint = document.getElementById('archiveHint');
    
    // Подсчитываем количество выбранных файлов
    let totalFiles = 0;
    selectedTemplatesForArchive.forEach(templateId => {
        const originalChecked = document.getElementById(`archive_original_${templateId}`)?.checked;
        const pdfChecked = document.getElementById(`archive_pdf_${templateId}`)?.checked;
        if (originalChecked) totalFiles++;
        if (pdfChecked) totalFiles++;
    });
    
    if (archiveBtn && archiveHint) {
        if (totalFiles === 0) {
            archiveBtn.disabled = true;
            archiveBtn.classList.add('disabled');
            archiveHint.textContent = 'Оберіть принаймні один файл для архіву';
        } else {
            archiveBtn.disabled = false;
            archiveBtn.classList.remove('disabled');
            archiveHint.textContent = `Завантажити ${totalFiles} ${totalFiles === 1 ? 'файл' : totalFiles < 5 ? 'файли' : 'файлів'}`;
        }
    }
}

// ========== СКАЧИВАНИЕ ДОКУМЕНТОВ ==========

async function downloadDocument(templateId, isPdf) {
    // Находим кнопку для показа индикатора загрузки
    const button = event.target;
    const originalText = button.innerHTML;
    
    try {
        // Показываем индикатор загрузки
        button.innerHTML = isPdf ? '⏳ Конвертація...' : '⏳ Генерація...';
        button.disabled = true;
        button.classList.add('loading');
        
        // Объединяем formData и calculatedValues для генерации
        const allData = { ...formData, ...calculatedValues };
        
        const response = await fetch(`/api/generate-document/${templateId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                formData: allData,
                convertToPdf: isPdf
            })
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            
            // Получаем имя файла из заголовка Content-Disposition
            const contentDisposition = response.headers.get('Content-Disposition');
            let fileName = `document_${templateId}.docx`;
            
            if (contentDisposition) {
                // Парсим RFC 5987 формат: filename*=UTF-8''encoded_name
                const filenameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/);
                if (filenameMatch) {
                    fileName = decodeURIComponent(filenameMatch[1]);
                }
            }
            
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showNotification('Документ успішно завантажено!', 'success');
        } else {
            showNotification('Помилка генерації документа', 'error');
        }
    } catch (error) {
        console.error('Помилка завантаження:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    } finally {
        // Возвращаем кнопку в нормальное состояние
        button.innerHTML = originalText;
        button.disabled = false;
        button.classList.remove('loading');
    }
}

async function downloadArchive() {
    const button = event.target;
    const originalText = button.innerHTML;
    
    try {
        // Проверяем, есть ли выбранные документы
        if (selectedTemplatesForArchive.size === 0) {
            showNotification('Оберіть принаймні один документ для архіву', 'error');
            return;
        }
        
        // Показываем индикатор загрузки
        button.innerHTML = '⏳ Створення архіву...';
        button.disabled = true;
        button.classList.add('loading');
        
        // Собираем ID только выбранных шаблонов
        const templateIds = Array.from(selectedTemplatesForArchive);
        
        // Собираем информацию о том, какие форматы нужны для каждого шаблона
        const originalFlags = {};
        const pdfFlags = {};
        templateIds.forEach(templateId => {
            originalFlags[templateId] = document.getElementById(`archive_original_${templateId}`)?.checked || false;
            pdfFlags[templateId] = document.getElementById(`archive_pdf_${templateId}`)?.checked || false;
        });
        
        // Объединяем formData и calculatedValues
        const allData = { ...formData, ...calculatedValues };
        
        const response = await fetch('/api/generate-archive', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                formData: allData,
                templateIds: templateIds,
                originalFlags: originalFlags,
                pdfFlags: pdfFlags
            })
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            
            // Получаем имя файла из заголовка
            const contentDisposition = response.headers.get('Content-Disposition');
            let fileName = 'Documents.zip';
            
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/);
                if (filenameMatch) {
                    fileName = decodeURIComponent(filenameMatch[1]);
                }
            }
            
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showNotification('Архів успішно завантажено!', 'success');
        } else {
            showNotification('Помилка генерації архіву', 'error');
        }
    } catch (error) {
        console.error('Помилка завантаження архіву:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    } finally {
        // Возвращаем кнопку в нормальное состояние
        button.innerHTML = originalText;
        button.disabled = false;
        button.classList.remove('loading');
    }
}

// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

function updateCheckButton() {
    const checkBtn = document.getElementById('checkAvailabilityBtn');
    const hasData = Object.values(formData).some(value => value && value.trim() !== '');
    checkBtn.disabled = !hasData;
}

function clearForm() {
    if (confirm('Ви впевнені, що хочете очистити форму?')) {
        formData = {};
        calculatedValues = {};
        
        // Очищаем все поля ввода
        document.querySelectorAll('input, textarea').forEach(input => {
            input.value = '';
        });
        
        // Очищаем вычисляемые поля
        document.querySelectorAll('[id^="calculated_"]').forEach(element => {
            element.textContent = 'Обчислюється автоматично...';
        });
        
        // Скрываем секцию документов
        document.getElementById('availableDocuments').style.display = 'none';
        
        // Обновляем кнопку
        updateCheckButton();
        
        showNotification('Форму очищено', 'info');
    }
}

function showNotification(message, type = 'info') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type}`;
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// ========== ЗУПИНКА СЕРВЕРА ==========

async function shutdownServer() {
    if (!confirm('Ви впевнені, що хочете зупинити сервер та закрити додаток?')) {
        return;
    }
    
    try {
        console.log('Відправка запиту на зупинку...');
        showNotification('Зупинка сервера...', 'info');
        
        const response = await fetch('/api/shutdown', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        console.log('Відповідь сервера:', response.status);
        
        if (response.ok) {
            // Показуємо повідомлення
            document.body.innerHTML = `
                <div style="display: flex; align-items: center; justify-content: center; height: 100vh; text-align: center; font-family: Arial, sans-serif;">
                    <div>
                        <h1 style="color: #28a745; margin-bottom: 20px;">✅ Сервер зупинено</h1>
                        <p style="font-size: 18px; color: #666;">Додаток успішно закрито</p>
                        <p style="font-size: 14px; color: #999; margin-top: 20px;">Можете закрити це вікно браузера</p>
                    </div>
                </div>
            `;
            
            // Закриваємо вкладку через 2 секунди
            setTimeout(() => {
                window.close();
            }, 2000);
        } else {
            showNotification('Помилка зупинки сервера (статус: ' + response.status + ')', 'error');
        }
        
    } catch (error) {
        console.error('Помилка зупинки сервера:', error);
        showNotification('Помилка: ' + error.message, 'error');
    }
}

// Обробник закриття вкладки (опціонально - можна закоментувати якщо не потрібно)
let isShuttingDown = false;

window.addEventListener('beforeunload', (e) => {
    // Закоментуйте наступні рядки якщо не хочете автоматичне закриття при закритті браузера
    /*
    if (!isShuttingDown && confirm('Зупинити сервер при закритті?')) {
        isShuttingDown = true;
        fetch('/api/shutdown', { method: 'POST', keepalive: true });
    }
    */
});
