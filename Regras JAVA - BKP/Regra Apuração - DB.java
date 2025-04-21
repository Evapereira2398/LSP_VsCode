package custom.DB.acerto;

import org.joda.time.LocalDate;

import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.entities.readonly.IR034FUN;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.Colaborador;
import com.senior.rule.Rule;

import custom.senior.apuracao.ConsistenciaAcertos;
import custom.senior.apuracao.acerto.ContextoConsistenciaAcerto;

@Rule(description = "Regra de Acertos")
public class RegraAcerto extends ConsistenciaAcertos {

	public void execute() {
		
		ContextoConsistenciaAcerto acerto = getContainer().getContextoConsistenciaAcerto();
		
		// Obtém informações do colaborador
		Colaborador colaborador = acerto.getColaborador();
		int numEmp = colaborador.getNumeroEmpresa();
		int tipCol = colaborador.getTipoColaborador();
		int numCad = colaborador.getNumCad();
		LocalDate datAdm = new LocalDate(); 
		
		// Obtém informações do período processado
		LocalDate datPro = acerto.getData();
		LocalDate iniAno = acerto.getDataFinal().withMonthOfYear(1).withDayOfMonth(1);
		
		// Inicialização variáveis
		MappedParamProvider paramProvider = new MappedParamProvider();
		int sitFolAni = 381;
		boolean folAni = false;					
	
		// Verifica se já teve a folga de aniversário no ano corrente
		paramProvider.setParam("numEmp", numEmp);
		paramProvider.setParam("tipCol", tipCol);
		paramProvider.setParam("numCad", numCad);
		paramProvider.setParam("iniAno", iniAno);
		paramProvider.setParam("datPro", datPro);
		paramProvider.setParam("codSit", sitFolAni);
		IR066SIT cSitFol = consulta(IR066SIT.class, "NumEmp=:numEmp AND " + "TipCol=:tipCol AND "
				+ "NumCad=:numCad AND " + "DatApu>=:iniAno AND "  + "DatApu<:datPro AND " + "CodSit=:codSit", paramProvider);
		if (cSitFol != null)
			folAni = true;	
		
		// Busca da data de admissão
		paramProvider.setParam("numEmp", numEmp);
		paramProvider.setParam("tipCol", tipCol);
		paramProvider.setParam("numCad", numCad);
		IR034FUN cDatAdm = consulta(IR034FUN.class, "NumEmp=:numEmp AND " + "TipCol=:tipCol AND "
				+ "NumCad=:numCad", paramProvider);
		if (cDatAdm != null)
			datAdm = cDatAdm.getDatAdm();	
	
		
		// Verifica se a admissão atende ao mínimo
		if (datAdm.isAfter(datPro.minusYears(2).withMonthOfYear(12).withDayOfMonth(31))) {
			folAni = true;
		}
		
		// Bloqueia o acerto caso admissão seja posterior ao mínimo ou já teve a situação no ano
		if (folAni && acerto.comparaSituacoes(sitFolAni)) {		
			acerto.mensagemLog("Acerto não permitido! Colaborador não elegível para a Folga de Aniversário ou já tirou no ano atual.");
		}
	}
	

	// Método genérico para fazer um cursor
	private <C extends IEntity> C consulta(Class<C> entidade, String filtro, MappedParamProvider paramProvider) {
		ICursor<C> cursor = getContainer().getEntitySession().newCursor(entidade);
		if (filtro != "")
			cursor.addFilter(filtro, paramProvider);
		cursor.open();
		try {
			if (cursor.first()) {
				C ler = cursor.read();
				return ler;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			cursor.close();
		}
		return null;
	}
	
	
	
	

}