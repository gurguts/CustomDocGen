// Глобальные переменные
let currentConfig = null;
let editingField = null;
let editingTemplate = null;
let uploadedFileName = null;

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadConfig();
    setupTabs();
    setupModals();
    setupForms();
});

// ========== ЗАГРУЗКА КОНФИГУРАЦИИ ==========

async function loadConfig() {
    try {
        const response = await fetch('/api/config');
        currentConfig = await response.json();
        renderFields();
        renderTemplates();
    } catch (error) {
        console.error('Помилка завантаження конфігурації:', error);
        showNotification('Помилка завантаження конфігурації', 'error');
    }
}

// ========== УПРАВЛЕНИЕ ВКЛАДКАМИ ==========

function setupTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;
            switchTab(tabName);
        });
    });
}

function switchTab(tabName) {
    // Убираем активный класс со всех вкладок
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    
    // Активируем выбранную вкладку
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`${tabName}-tab`).classList.add('active');
}

// ========== ОТОБРАЖЕНИЕ ПОЛЕЙ ==========

function renderFields() {
    const fieldsList = document.getElementById('fieldsList');
    
    if (!currentConfig.fields || currentConfig.fields.length === 0) {
        fieldsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📝</div>
                <div class="empty-state-text">Поки немає полів</div>
                <div class="empty-state-hint">Натисніть "Додати поле" для створення першого поля</div>
            </div>
        `;
        return;
    }
    
    // Сортируем поля по order
    const sortedFields = [...currentConfig.fields].sort((a, b) => a.order - b.order);
    
    fieldsList.innerHTML = sortedFields.map(field => `
        <div class="item-card">
            <div class="item-header">
                <div class="item-title">${field.displayName}</div>
                <div class="item-actions">
                    <button class="btn-icon btn-edit" onclick="editField('${field.id}')">✏️ Редагувати</button>
                    <button class="btn-icon btn-delete" onclick="deleteField('${field.id}')">🗑️ Видалити</button>
                </div>
            </div>
            <div class="item-details">
                <div class="detail-item">
                    <span class="detail-label">Плейсхолдер:</span>
                    <span class="detail-value">${field.placeholder}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">ID поля:</span>
                    <span class="detail-value">${field.id}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Тип:</span>
                    <span class="detail-value"><span class="badge badge-type">${field.fieldType}</span></span>
                </div>
                ${field.fieldType === 'formula' && field.formula ? `
                <div class="detail-item" style="grid-column: 1 / -1;">
                    <span class="detail-label">Формула:</span>
                    <span class="detail-value" style="font-family: monospace; background: #f5f5f5; padding: 4px 8px; border-radius: 4px;">${field.formula}</span>
                </div>
                ${field.decimalPlaces !== null && field.decimalPlaces !== undefined ? `
                <div class="detail-item">
                    <span class="detail-label">Формат:</span>
                    <span class="detail-value">${field.decimalPlaces === 0 ? 'Ціле число' : field.decimalPlaces + ' знаків після коми'}</span>
                </div>
                ` : ''}
                ` : ''}
                <div class="detail-item">
                    <span class="detail-label">Порядок:</span>
                    <span class="detail-value">${field.order}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Обов'язкове:</span>
                    <span class="detail-value">
                        <span class="badge ${field.required ? 'badge-required' : 'badge-optional'}">
                            ${field.required ? 'Так' : 'Ні'}
                        </span>
                    </span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Запам'ятовувати значення:</span>
                    <span class="detail-value">
                        <span class="badge ${field.rememberValues ? 'badge-required' : 'badge-optional'}">
                            ${field.rememberValues ? 'Так' : 'Ні'}
                        </span>
                    </span>
                </div>
            </div>
        </div>
    `).join('');
}

// ========== ОТОБРАЖЕНИЕ ШАБЛОНОВ ==========

function renderTemplates() {
    const templatesList = document.getElementById('templatesList');
    
    if (!currentConfig.templates || currentConfig.templates.length === 0) {
        templatesList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📄</div>
                <div class="empty-state-text">Поки немає шаблонів</div>
                <div class="empty-state-hint">Натисніть "Додати шаблон" для створення першого шаблону</div>
            </div>
        `;
        return;
    }
    
    templatesList.innerHTML = currentConfig.templates.map(template => {
        const requiredFieldsNames = template.requiredFieldIds
            .map(fieldId => {
                const field = currentConfig.fields.find(f => f.id === fieldId);
                return field ? field.displayName : fieldId;
            })
            .join(', ');
        
        return `
            <div class="item-card">
                <div class="item-header">
                    <div class="item-title">${template.displayName}</div>
                    <div class="item-actions">
                        <button class="btn-icon btn-edit" onclick="editTemplate('${template.id}')">✏️ Редагувати</button>
                        <button class="btn-icon btn-delete" onclick="deleteTemplate('${template.id}')">🗑️ Видалити</button>
                    </div>
                </div>
                <div class="item-details">
                    <div class="detail-item">
                        <span class="detail-label">ID шаблону:</span>
                        <span class="detail-value">${template.id}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Файл:</span>
                        <span class="detail-value">${template.fileName}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Патерн завантаження:</span>
                        <span class="detail-value">${template.downloadPattern}</span>
                    </div>
                    <div class="detail-item" style="grid-column: 1 / -1;">
                        <span class="detail-label">Обов'язкові поля:</span>
                        <span class="detail-value">${requiredFieldsNames || 'Немає'}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// ========== УПРАВЛЕНИЕ МОДАЛЬНЫМИ ОКНАМИ ==========

function setupModals() {
    // Кнопки открытия модальных окон
    document.getElementById('addFieldBtn').addEventListener('click', () => openFieldModal());
    document.getElementById('addTemplateBtn').addEventListener('click', () => openTemplateModal());
    
    // Закрытие модальных окон
    document.querySelectorAll('.close, .close-modal').forEach(btn => {
        btn.addEventListener('click', function() {
            const modal = this.closest('.modal');
            closeModal(modal);
        });
    });
    
    // Закрытие при клике вне модального окна
    window.addEventListener('click', (event) => {
        if (event.target.classList.contains('modal')) {
            closeModal(event.target);
        }
    });
}

function openFieldModal(fieldId = null) {
    const modal = document.getElementById('fieldModal');
    const form = document.getElementById('fieldForm');
    const title = document.getElementById('fieldModalTitle');
    
    form.reset();
    editingField = fieldId;
    
    if (fieldId) {
        title.textContent = 'Редагувати поле';
        const field = currentConfig.fields.find(f => f.id === fieldId);
        if (field) {
            document.getElementById('fieldId').value = field.id;
            document.getElementById('fieldDisplayName').value = field.displayName;
            document.getElementById('fieldPlaceholder').value = field.placeholder;
            document.getElementById('fieldIdInput').value = field.id;
            document.getElementById('fieldType').value = field.fieldType;
            document.getElementById('fieldOrder').value = field.order;
            document.getElementById('fieldRequired').checked = field.required;
            document.getElementById('fieldRememberValues').checked = field.rememberValues || false;
            document.getElementById('fieldIdInput').disabled = true;
            
            // Заполняем формулу если есть
            if (field.formula) {
                document.getElementById('fieldFormula').value = field.formula;
            }
            
            // Заполняем количество десятичных знаков если есть
            if (field.decimalPlaces !== null && field.decimalPlaces !== undefined) {
                document.getElementById('fieldDecimalPlaces').value = field.decimalPlaces;
            }
        }
    } else {
        title.textContent = 'Добавить поле';
        document.getElementById('fieldOrder').value = (currentConfig.fields.length || 0) + 1;
        document.getElementById('fieldIdInput').disabled = false;
    }
    
    // Показываем/скрываем поле формулы
    toggleFormulaField();
    
    modal.classList.add('show');
}

function openTemplateModal(templateId = null) {
    const modal = document.getElementById('templateModal');
    const form = document.getElementById('templateForm');
    const title = document.getElementById('templateModalTitle');
    
    form.reset();
    uploadedFileName = null;
    editingTemplate = templateId;
    
    // Заполняем список полей для чекбоксов
    renderRequiredFieldsCheckboxes();
    
    if (templateId) {
        title.textContent = 'Редагувати шаблон';
        const template = currentConfig.templates.find(t => t.id === templateId);
        if (template) {
            document.getElementById('templateId').value = template.id;
            document.getElementById('templateDisplayName').value = template.displayName;
            document.getElementById('templateIdInput').value = template.id;
            document.getElementById('downloadPattern').value = template.downloadPattern;
            document.getElementById('currentFileName').textContent = `Поточний файл: ${template.fileName}`;
            uploadedFileName = template.fileName;
            document.getElementById('templateIdInput').disabled = true;
            
            // Отмечаем выбранные поля
            template.requiredFieldIds.forEach(fieldId => {
                const checkbox = document.getElementById(`field-${fieldId}`);
                if (checkbox) checkbox.checked = true;
            });
        }
    } else {
        title.textContent = 'Добавить шаблон';
        document.getElementById('currentFileName').textContent = '';
        document.getElementById('templateIdInput').disabled = false;
    }
    
    modal.classList.add('show');
}

function renderRequiredFieldsCheckboxes() {
    const container = document.getElementById('requiredFieldsCheckboxes');
    
    if (!currentConfig.fields || currentConfig.fields.length === 0) {
        container.innerHTML = '<p style="color: #999;">Спочатку створіть поля</p>';
        return;
    }
    
    const sortedFields = [...currentConfig.fields].sort((a, b) => a.order - b.order);
    
    container.innerHTML = sortedFields.map(field => `
        <label class="checkbox-label">
            <input type="checkbox" id="field-${field.id}" value="${field.id}">
            ${field.displayName}
        </label>
    `).join('');
}

function closeModal(modal) {
    modal.classList.remove('show');
}

// ========== ОБРАБОТКА ФОРМ ==========

function setupForms() {
    // Форма поля
    document.getElementById('fieldForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveField();
    });
    
    // Форма шаблона
    document.getElementById('templateForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveTemplate();
    });
    
    // Загрузка файла шаблона
    document.getElementById('templateFile').addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (file) {
            await uploadTemplateFile(file);
        }
    });
}

// ========== ОПЕРАЦИИ С ПОЛЯМИ ==========

async function saveField() {
    const fieldType = document.getElementById('fieldType').value;
    
    // Получаем значение decimalPlaces
    const decimalPlacesInput = document.getElementById('fieldDecimalPlaces').value;
    const decimalPlaces = decimalPlacesInput !== '' ? parseInt(decimalPlacesInput) : null;
    
    const field = {
        id: document.getElementById('fieldIdInput').value,
        displayName: document.getElementById('fieldDisplayName').value,
        placeholder: document.getElementById('fieldPlaceholder').value,
        fieldType: fieldType,
        order: parseInt(document.getElementById('fieldOrder').value),
        required: document.getElementById('fieldRequired').checked,
        rememberValues: document.getElementById('fieldRememberValues').checked,
        formula: fieldType === 'formula' ? document.getElementById('fieldFormula').value : null,
        isCalculated: fieldType === 'formula',
        decimalPlaces: fieldType === 'formula' ? decimalPlaces : null
    };
    
    try {
        const url = editingField ? `/api/fields/${editingField}` : '/api/fields';
        const method = editingField ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(field)
        });
        
        if (response.ok) {
            showNotification('Поле успішно збережено', 'success');
            closeModal(document.getElementById('fieldModal'));
            await loadConfig();
        } else {
            showNotification('Помилка збереження поля', 'error');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    }
}

function editField(fieldId) {
    openFieldModal(fieldId);
}

async function deleteField(fieldId) {
    if (!confirm('Ви впевнені, що хочете видалити це поле?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/fields/${fieldId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showNotification('Поле успішно видалено', 'success');
            await loadConfig();
        } else {
            showNotification('Помилка видалення поля', 'error');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    }
}

// ========== ОПЕРАЦИИ С ШАБЛОНАМИ ==========

async function uploadTemplateFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    try {
        const response = await fetch('/api/templates/upload', {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        if (response.ok) {
            uploadedFileName = result.fileName;
            document.getElementById('currentFileName').textContent = `Завантажено: ${result.fileName}`;
            showNotification('Файл успішно завантажено', 'success');
        } else {
            showNotification(result.error || 'Помилка завантаження файлу', 'error');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    }
}

async function saveTemplate() {
    if (!uploadedFileName && !editingTemplate) {
        showNotification('Завантажте файл шаблону', 'error');
        return;
    }
    
    const selectedFields = Array.from(document.querySelectorAll('#requiredFieldsCheckboxes input:checked'))
        .map(checkbox => checkbox.value);
    
    if (selectedFields.length === 0) {
        showNotification('Оберіть хоча б одне обов\'язкове поле', 'error');
        return;
    }
    
    const template = {
        id: document.getElementById('templateIdInput').value,
        displayName: document.getElementById('templateDisplayName').value,
        fileName: uploadedFileName || currentConfig.templates.find(t => t.id === editingTemplate)?.fileName,
        downloadPattern: document.getElementById('downloadPattern').value,
        requiredFieldIds: selectedFields
    };
    
    try {
        const url = editingTemplate ? `/api/templates/${editingTemplate}` : '/api/templates';
        const method = editingTemplate ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(template)
        });
        
        if (response.ok) {
            showNotification('Шаблон успішно збережено', 'success');
            closeModal(document.getElementById('templateModal'));
            await loadConfig();
        } else {
            showNotification('Помилка збереження шаблону', 'error');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    }
}

function editTemplate(templateId) {
    openTemplateModal(templateId);
}

async function deleteTemplate(templateId) {
    if (!confirm('Ви впевнені, що хочете видалити цей шаблон? Файл шаблону також буде видалено.')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/templates/${templateId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showNotification('Шаблон успішно видалено', 'success');
            await loadConfig();
        } else {
            showNotification('Помилка видалення шаблону', 'error');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showNotification('Помилка з\'єднання з сервером', 'error');
    }
}

// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

function toggleFormulaField() {
    const fieldType = document.getElementById('fieldType').value;
    const formulaGroup = document.getElementById('formulaGroup');
    const decimalPlacesGroup = document.getElementById('decimalPlacesGroup');
    
    if (fieldType === 'formula') {
        formulaGroup.style.display = 'block';
        decimalPlacesGroup.style.display = 'block';
        document.getElementById('fieldFormula').required = true;
    } else {
        formulaGroup.style.display = 'none';
        decimalPlacesGroup.style.display = 'none';
        document.getElementById('fieldFormula').required = false;
    }
}

// ========== УВЕДОМЛЕНИЯ ==========

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

