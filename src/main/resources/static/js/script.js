// Элементы формы
const form = document.getElementById('documentForm');
const downloadInvoiceBtn = document.getElementById('downloadInvoiceBtn');
const clearFormBtn = document.getElementById('clearFormBtn');

// Поля, необходимые для Инвойса
const invoiceRequiredFields = [
    'contractNumber',
    'contractDate',
    'vehicleNumber',
    'weight',
    'consigneeName',
    'consigneeAddress',
    'productNameEn',
    'productNameUk'
];

// Функция для сбора данных из формы
function getFormData() {
    return {
        contractNumber: document.getElementById('contractNumber').value,
        contractDate: document.getElementById('contractDate').value,
        vehicleNumber: document.getElementById('vehicleNumber').value,
        weight: document.getElementById('weight').value,
        consigneeName: document.getElementById('consigneeName').value,
        consigneeAddress: document.getElementById('consigneeAddress').value,
        productNameEn: document.getElementById('productNameEn').value,
        productNameUk: document.getElementById('productNameUk').value,
        receiverName: document.getElementById('receiverName').value,
        receiverAddress: document.getElementById('receiverAddress').value,
        batchNumber: document.getElementById('batchNumber').value,
        unloadingPlace: document.getElementById('unloadingPlace').value,
        unloadingCountry: document.getElementById('unloadingCountry').value
    };
}

// Проверка доступности кнопки Инвойса
function checkInvoiceAvailability() {
    const formData = getFormData();
    
    // Проверка на фронтенде
    const isComplete = invoiceRequiredFields.every(field => {
        return formData[field] && formData[field].trim() !== '';
    });
    
    downloadInvoiceBtn.disabled = !isComplete;
}

// Добавление слушателей на все поля формы
const formInputs = form.querySelectorAll('input, textarea');
formInputs.forEach(input => {
    input.addEventListener('input', checkInvoiceAvailability);
});

// Скачивание Инвойса
downloadInvoiceBtn.addEventListener('click', async () => {
    const formData = getFormData();
    
    // Показываем состояние загрузки
    downloadInvoiceBtn.classList.add('loading');
    downloadInvoiceBtn.textContent = '⏳ Генерация документа...';
    
    try {
        const response = await fetch('/api/generate-invoice', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `Invoice_${formData.contractNumber}.docx`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showNotification('Документ успешно сгенерирован и скачан!', 'success');
        } else {
            showNotification('Ошибка при генерации документа', 'error');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    } finally {
        // Возвращаем кнопку в нормальное состояние
        downloadInvoiceBtn.classList.remove('loading');
        downloadInvoiceBtn.textContent = '📄 Скачать Инвойс';
    }
});

// Очистка формы
clearFormBtn.addEventListener('click', () => {
    if (confirm('Вы уверены, что хотите очистить форму?')) {
        form.reset();
        checkInvoiceAvailability();
        showNotification('Форма очищена', 'info');
    }
});

// Функция для показа уведомлений
function showNotification(message, type = 'info') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type}`;
    
    // Показываем уведомление
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    // Скрываем через 3 секунды
    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// Проверка при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    checkInvoiceAvailability();
    showNotification('Добро пожаловать! Заполните форму для генерации документов.', 'info');
});

